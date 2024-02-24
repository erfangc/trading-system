package com.example.tradingsystem.oms

import com.example.tradingsystem.ledger.LedgerService
import com.example.tradingsystem.ledger.Transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import quickfix.Message
import quickfix.field.*

@Service
class ExecutionReportListener(
    private val ledgerService: LedgerService,
    private val updateOrderService: UpdateOrderService,
) {
    
    private val log = LoggerFactory.getLogger(ExecutionReportListener::class.java)
    
    fun onExecutionReport(message: Message) {
        
        val clOrdId = message.getField(ClOrdID())
        val execId = message.getField(ExecID())
        val ordStatus = message.getField(OrdStatus())
        val lastPx = message.getField(LastPx())
        val leavesQty = message.getField(LeavesQty())
        val symbol = message.getField(Symbol())
        val lastQty = message.getField(LastQty())
        
        log.info(
            "Execution report received for " +
                    "clOrdId=${clOrdId.value} " +
                    "execId=${execId.value} " +
                    "symbol=${symbol.value} " +
                    "ordStatus=${ordStatus.value} " +
                    "lastPx=${lastPx.value} " +
                    "lastQty=${lastQty.value}"
        )
        
        val transaction = ledgerService.createTransaction(
            Transaction(
                counterpartyCompId = "EXCHANGE",
                counterpartyExecId = execId.value,
                orderId = clOrdId.value,
                securityId = symbol.value,
                qty = lastQty.value,
                price = lastPx.value,
                type = "TRADE",
            )
        )
        
        val newStatus = if (leavesQty.value == 0.0) "FILLED" else "PARTIALLY_FILLED"
        updateOrderService.updateOrderStatus(orderId = clOrdId.value, newStatus = newStatus)
        
        log.info("Created transaction=$transaction from execution report clOrdId=${clOrdId.value} execId=${execId.value}")
    }

}