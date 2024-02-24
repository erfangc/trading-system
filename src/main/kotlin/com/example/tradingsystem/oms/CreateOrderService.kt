package com.example.tradingsystem.oms

import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import quickfix.Message
import quickfix.Session
import quickfix.SessionID
import quickfix.field.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Service
class CreateOrderService(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val orderValidator: OrderValidator,
) {
    
    private val log = LoggerFactory.getLogger(CreateOrderService::class.java)

    fun createOrder(order: Order): Order {
        val accountNumber = order.accountNumber
        val securityId = order.securityId
        val type = order.type ?: "MARKET"
        val timeInForce = order.timeInForce ?: "GTC"
        val qty = order.qty
        val orderId = UUID.randomUUID().toString()

        val ret = order.copy(
            id = orderId,
            date = LocalDate.now(),
            timestamp = Instant.now(),
            type = type,
            timeInForce = timeInForce,
            status = "SUBMITTED",
        )
        orderValidator.validateOrderOrThrow(ret)
        
        insertIntoOrdersTable(ret, orderId, qty, securityId, accountNumber, type, timeInForce)
        sendToCounterPartyViaFIX(ret)
        return ret
    }
    
    private fun insertIntoOrdersTable(
        ret: Order,
        orderId: String,
        qty: Double?,
        securityId: String?,
        accountNumber: String?,
        type: String,
        timeInForce: String
    ) {
        namedParameterJdbcTemplate.update(
            """
            insert into orders (id, account_number, security_id, type, time_in_force, limit_price, qty, date, timestamp, status)
            values (:id, :account_number, :security_id, :type, :time_in_force, :limit_price, :qty, :date, :timestamp, :status)
            """.trimIndent(),
            mapOf(
                "id" to ret.id,
                "account_number" to ret.accountNumber,
                "security_id" to ret.securityId,
                "type" to ret.type,
                "time_in_force" to ret.timeInForce,
                "limit_price" to ret.limitPrice,
                "qty" to ret.qty,
                "date" to ret.date,
                "timestamp" to ret.timestamp,
                "status" to ret.status,
            )
        )
        log.info("Order inserted orderId=$orderId qty=$qty securityId=$securityId accountNumber=$accountNumber type=$type timeInForce=$timeInForce")
    }

    fun sendToCounterPartyViaFIX(order: Order) {
        if (order.qty == null || order.qty == 0.0) {
            throw RuntimeException("order ${order.id} has a blank or 0.0 qty, which cannot be submitted for execution")
        }
        val newOrderSingle = Message()
        newOrderSingle.header.setString(MsgType.FIELD, MsgType.ORDER_SINGLE)
        newOrderSingle.setField(ClOrdID(order.id))
        newOrderSingle.setField(HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION))
        newOrderSingle.setField(Symbol(order.securityId))
        newOrderSingle.setField(Side(if (order.qty > 0) '1' else '2'))
        newOrderSingle.setField(TransactTime())
        newOrderSingle.setField(OrdType('1'))
        newOrderSingle.setField(OrderQty(order.qty))

        val result = Session.sendToTarget(newOrderSingle, FIXConnection.sessionID)
        log.info("Sent FIX.4.4 message=$newOrderSingle result=$result")
    }

}