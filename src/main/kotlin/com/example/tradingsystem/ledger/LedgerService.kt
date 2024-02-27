package com.example.tradingsystem.ledger

import com.example.tradingsystem.oms.GetOrdersService
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Service
class LedgerService(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val getOrdersService: GetOrdersService,
) {

    private val log = LoggerFactory.getLogger(LedgerService::class.java)
    private val firmHouseAccount = "C00001"

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun createCashTransaction(accountNumber: String, amount: Double) {
        updatePosition(accountNumber = accountNumber, securityId = "USD.CASH", qty = amount)
        updatePosition(accountNumber = firmHouseAccount, securityId = "USD.CASH", qty = -amount)
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun createTransaction(transaction: Transaction): Transaction {

        val price = transaction.price ?: 0.0
        val qty = transaction.qty ?: 0.0
        val cashImpact = price * qty
        val accountNumber = determineAccountNumber(transaction)
        val securityId = transaction.securityId ?: error("securityId cannot be missing")
        val type = transaction.type ?: error("type cannot be missing")

        val ret = transaction.copy(
            id = UUID.randomUUID().toString(),
            accountNumber = accountNumber,
            contraAccountNumber = firmHouseAccount,
            cashImpact = cashImpact,
            status = "CONFIRMED",
            effectiveDate = LocalDate.now(),
            settleDate = LocalDate.now().plusDays(3),
            createdAt = Instant.now(),
        )

        val start = System.currentTimeMillis()
        updatePosition(accountNumber = accountNumber, securityId = securityId, qty = qty)
        updatePosition(accountNumber = firmHouseAccount, securityId = securityId, qty = -qty)
        updatePosition(accountNumber = accountNumber, securityId = "USD.CASH", qty = -cashImpact)
        updatePosition(accountNumber = firmHouseAccount, securityId = "USD.CASH", qty = cashImpact)
        val insertPositionsStop = System.currentTimeMillis()
        insertTransaction(ret)
        val stop = System.currentTimeMillis()
        log.info("Transaction posting took ${stop - start}ms position updates took ${insertPositionsStop - start}ms transactionId=${ret.id} accountNumber=$accountNumber securityId=$securityId type=$type")

        return ret
    }

    private fun determineAccountNumber(transaction: Transaction): String {
        if (transaction.accountNumber == null && transaction.orderId != null) {
            val order = getOrdersService.getOrder(transaction.orderId)
            return order?.accountNumber
                ?: error("accountNumber cannot be missing and the referenced orderId must have a valid accountNumber")
        } else {
            return transaction.accountNumber ?: error("accountNumber cannot be missing")
        }
    }

    private fun insertTransaction(transaction: Transaction) {
        namedParameterJdbcTemplate.update(
            """
            insert into transactions (id, type, counterparty_comp_id, counterparty_exec_id, order_id, account_number, contra_account_number, security_id, status, qty, price, cash_impact, fee, effective_date, settle_date, created_at, memo) 
            values (:id, :type, :counterparty_comp_id, :counterparty_exec_id, :order_id, :account_number, :contra_account_number, :security_id, :status, :qty, :price, :cash_impact, :fee, :effective_date, :settle_date, :created_at, :memo)
            """.trimIndent(),
            mapOf(
                "id" to transaction.id,
                "type" to transaction.type,
                "counterparty_comp_id" to transaction.counterpartyCompId,
                "counterparty_exec_id" to transaction.counterpartyExecId,
                "order_id" to transaction.orderId,
                "account_number" to transaction.accountNumber,
                "contra_account_number" to transaction.contraAccountNumber,
                "security_id" to transaction.securityId,
                "status" to transaction.status,
                "qty" to transaction.qty,
                "price" to transaction.price,
                "cash_impact" to transaction.cashImpact,
                "fee" to transaction.fee,
                "effective_date" to transaction.effectiveDate,
                "settle_date" to transaction.settleDate,
                "created_at" to transaction.createdAt,
                "memo" to transaction.memo,
            )
        )
    }

    @Transactional
    fun updatePosition(accountNumber: String, securityId: String, qty: Double) {
        namedParameterJdbcTemplate.update(
            """
            insert into positions (account_number, security_id, qty) 
            values (:account_number, :security_id, :qty) on duplicate key update qty = qty + :qty
            """.trimIndent(),
            mapOf(
                "account_number" to accountNumber,
                "security_id" to securityId,
                "qty" to qty
            )
        )

        /*
        Update the historical_positions table
        - If there is an open position with a start_date < today, close that position and start a new one
        - If there is an open position with a start_date = today - update it
        - If there is no open positions create a new one 
         */
        val existingQty = try {
            val existingQty = namedParameterJdbcTemplate.queryForObject(
                """
            select qty from historical_positions 
            where account_number = :account_number 
            and security_id = :security_id
            and start_date < curdate()
            and stop_date = '3999-12-31'
            """.trimIndent(),
                mapOf(
                    "account_number" to accountNumber,
                    "security_id" to securityId,
                ),
                Double::class.java
            )

            namedParameterJdbcTemplate.update(
                """
            update historical_positions set stop_date = curdate() 
            where account_number = :account_number 
            and security_id = :security_id
            and stop_date = '3999-12-31'
            """.trimIndent(),
                mapOf(
                    "account_number" to accountNumber,
                    "security_id" to securityId,
                )
            )
            existingQty
        } catch (e: EmptyResultDataAccessException) {
            null
        }



        namedParameterJdbcTemplate.update(
            """
            insert into historical_positions (account_number, security_id, qty, start_date, stop_date) 
            values (:account_number, :security_id, :new_qty, curdate(), '3999-12-31')
            on duplicate key update qty = qty + :qty
            """.trimIndent(),
            mapOf(
                "account_number" to accountNumber,
                "security_id" to securityId,
                "new_qty" to (existingQty ?: 0.0) + qty,
                "qty" to qty
            )
        )
    }

    fun getTransactions(accountNumber: String): List<Transaction> {
        return namedParameterJdbcTemplate.query(
            "select * from transactions where account_number = :account_number",
            mapOf("account_number" to accountNumber)
        ) { rs, _ ->
            Transaction(
                id = rs.getString("id"),
                counterpartyCompId = rs.getString("counterparty_comp_id"),
                counterpartyExecId = rs.getString("counterparty_exec_id"),
                orderId = rs.getString("order_id"),
                accountNumber = rs.getString("account_number"),
                contraAccountNumber = rs.getString("contra_account_number"),
                securityId = rs.getString("security_id"),
                type = rs.getString("type"),
                status = rs.getString("status"),
                qty = rs.getDouble("qty"),
                price = rs.getDouble("price"),
                cashImpact = rs.getDouble("cash_impact"),
                fee = rs.getDouble("fee"),
                effectiveDate = rs.getDate("effective_date").toLocalDate(),
                settleDate = rs.getDate("settle_date").toLocalDate(),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                memo = rs.getString("memo"),
            )
        }
    }

    fun getPositions(accountNumber: String): List<Position> {
        return namedParameterJdbcTemplate.query(
            """
                select p.account_number,
                       security_id,
                       qty,
                       IF(security_id = 'USD.CASH', qty, px.close_price * qty) as market_value,
                       daily_change_percent,
                       daily_change,
                       updated,
                       close_price
                from positions p
                         left join prices px on p.security_id = px.ticker
                where account_number = :account_number
            """.trimIndent(),
            mapOf("account_number" to accountNumber)
        ) { rs, _ ->
            Position(
                accountNumber = rs.getString("account_number"),
                securityId = rs.getString("security_id"),
                qty = rs.getDouble("qty"),
                marketValue = rs.getDouble("market_value"),
                dailyChangePercent = rs.getDouble("daily_change_percent"),
                dailyChange = rs.getDouble("daily_change"),
                updated = rs.getTimestamp("updated")?.toInstant(),
                closePrice = rs.getDouble("close_price"),
            )
        }
    }

}