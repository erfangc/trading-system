package com.example.tradingsystem.oms

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class GetOrdersService(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
    
    fun getOrder(orderId: String): Order? {
        return namedParameterJdbcTemplate.queryForObject(
            """
            select * from orders where id = :id
            """.trimIndent(),
            mapOf("id" to orderId), orderRowMapper
        )
    }

    private val orderRowMapper = RowMapper { rs, _ ->
        Order(
            id = rs.getString("id"),
            accountNumber = rs.getString("account_number"),
            securityId = rs.getString("security_id"),
            type = rs.getString("type"),
            timeInForce = rs.getString("time_in_force"),
            limitPrice = rs.getDouble("limit_price"),
            qty = rs.getDouble("qty"),
            timestamp = rs.getTimestamp("timestamp").toInstant(),
            date = rs.getDate("date").toLocalDate(),
            status = rs.getString("status"),
        )
    }

    fun getOrders(accountNumber: String): List<Order> {
        return namedParameterJdbcTemplate.query(
            """
            select * from orders where account_number = :account_number
            """.trimIndent(),
            mapOf("account_number" to accountNumber), 
            orderRowMapper
        )
    }

}