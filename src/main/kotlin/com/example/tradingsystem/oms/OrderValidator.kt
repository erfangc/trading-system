package com.example.tradingsystem.oms

import com.example.tradingsystem.ValidationException
import com.example.tradingsystem.alphavantage.AlphaVantageService
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class OrderValidator(
    private val alphaVantageService: AlphaVantageService,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    data class ValidationError(
        val code: String? = null,
        val message: String? = null,
        val field: String? = null,
    )

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
            try {
                val cash = namedParameterJdbcTemplate.queryForObject(
                    "select qty from positions where account_number = :account_number and security_id = 'USD.CASH'",
                    mapOf("account_number" to order.accountNumber),
                    Double::class.java
                )
                if (securityId != null && qty != null && cash != null) {
                    val quote = alphaVantageService.getQuote(securityId)
                    if (quote == null) {
                        errors.add(ValidationError(message = "security_id is not valid for trading"))
                    } else {
                        val cashNeededEstimate = quote * qty
                        if (cash < cashNeededEstimate) {
                            errors.add(ValidationError(message = "account ${order.accountNumber} has insufficient cash to cover the trade"))
                        }
                    }
                }
            } catch (e: EmptyResultDataAccessException) {
                errors.add(ValidationError(message = "account ${order.accountNumber} has insufficient cash to cover the trade"))
            }
        }
        return errors
    }

    private fun validateAccount(ret: Order): List<ValidationError> {
        if (ret.accountNumber == null) {
            return listOf(ValidationError(message = "account_number is missing on the order"))
        }
        return try {
            namedParameterJdbcTemplate.queryForRowSet(
                "select * from account where account_number = :account_number;",
                mapOf("account_number" to ret.accountNumber)
            )
            emptyList()
        } catch (e: EmptyResultDataAccessException) {
            listOf(ValidationError(message = "account ${ret.accountNumber} cannot be found"))
        }
    }
}