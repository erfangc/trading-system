package com.example.tradingsystem.oms

import com.example.tradingsystem.ValidationError
import com.example.tradingsystem.ValidationException
import com.example.tradingsystem.polygon.PolygonService
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class OrderValidator(
    private val polygonService: PolygonService,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun validateOrderOrThrow(ret: Order) {
        val validationErrors = validateAccount(ret) + validateSufficientCash(ret)
        if (validationErrors.isNotEmpty()) {
            throw ValidationException(validationErrors = validationErrors)
        }
    }

    private fun validateSufficientCash(order: Order): List<ValidationError> {
        val qty = order.qty
        val securityId = order.securityId

        val errors = arrayListOf<ValidationError>()

        if (securityId == null) {
            errors.add(ValidationError(message = "security_id is missing on the order"))
        }

        if (qty == null) {
            errors.add(ValidationError(message = "qty is missing on the order"))
        }

        if ((qty ?: 0.0) > 0.0) {
            val cash = try {
                namedParameterJdbcTemplate.queryForObject(
                    "select qty from positions where account_number = :account_number and security_id = 'USD.CASH'",
                    mapOf("account_number" to order.accountNumber),
                    Double::class.java
                )
            } catch (e: EmptyResultDataAccessException) {
                0.0
            }
            if (securityId != null && qty != null && cash != null) {
                val previousClose = polygonService.previousClose(securityId)
                val quote = previousClose.results?.get(0)?.c
                if (quote == null) {
                    errors.add(ValidationError(message = "security_id is not valid for trading"))
                } else {
                    val cashNeededEstimate = quote * qty
                    if (cash < cashNeededEstimate) {
                        errors.add(ValidationError(message = "account ${order.accountNumber} has insufficient cash to cover the trade"))
                    }
                }
            }
        }
        return errors
    }

    private fun validateAccount(ret: Order): List<ValidationError> {
        val errors = arrayListOf<ValidationError>()
        if (ret.accountNumber == null) {
            errors.add(ValidationError(message = "account_number is missing on the order"))
        } else {
            val results = namedParameterJdbcTemplate.queryForList(
                "select * from account where account_number = :account_number;",
                mapOf("account_number" to ret.accountNumber)
            )
            if (results.size == 0)
                errors.add(ValidationError(message = "account ${ret.accountNumber} is not a valid account"))
        }
        return errors
    }
}