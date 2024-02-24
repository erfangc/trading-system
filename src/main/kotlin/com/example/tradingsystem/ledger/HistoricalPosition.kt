package com.example.tradingsystem.ledger

import java.time.LocalDate

data class HistoricalPosition(
    val accountNumber: String? = null,
    val securityId: String? = null,
    val qty: Double? = null,
    val settledQty: Double? = null,
    val startDate: LocalDate? = null,
    val stopDate: LocalDate? = null,
)