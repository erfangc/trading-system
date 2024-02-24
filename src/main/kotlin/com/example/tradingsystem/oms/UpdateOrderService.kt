package com.example.tradingsystem.oms

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class UpdateOrderService(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
    fun updateOrderStatus(orderId: String, newStatus: String) {
        namedParameterJdbcTemplate.update(
            """
            update orders set status = :status where id = :id    
            """.trimIndent(),
            mapOf(
                "id" to orderId,
                "status" to newStatus,
            )
        )
    }
}