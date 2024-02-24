package com.example.tradingsystem.ledger

import java.time.Instant
import java.time.LocalDate

data class Transaction(
    val id: String? = null,
    val type: String? = null,
    val counterpartyCompId: String? = null,
    val counterpartyExecId: String? = null,
    val orderId: String? = null,
    val accountNumber: String? = null,
    val contraAccountNumber: String? = null,
    val securityId: String? = null,
    val status: String? = null,
    val qty: Double? = null,
    val price: Double? = null,
    val cashImpact: Double? = null,
    val fee: Double? = null,
    val effectiveDate: LocalDate? = null,
    val settleDate: LocalDate? = null,
    val createdAt: Instant? = null,
    val memo: String? = null,
)