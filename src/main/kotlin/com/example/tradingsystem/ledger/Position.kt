package com.example.tradingsystem.ledger

import java.time.LocalDate

data class Position(
    val accountNumber: String? = null,
    val securityId: String? = null,
    val qty: Double? = null,
    val settledQty: Double? = null,
    val effectiveDate: LocalDate? = null,
    val marketValue: Double? = null,
    val settledMarketValue: Double? = null,
    val costBasis: LocalDate? = null,
)