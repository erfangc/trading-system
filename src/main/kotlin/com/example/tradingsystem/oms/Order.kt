package com.example.tradingsystem.oms

import java.time.Instant
import java.time.LocalDate

data class Order(
    val id: String? = null,
    val accountNumber: String? = null,
    val securityId: String? = null,
    /*
    LIMIT/MARKET
     */
    val type: String? = null,
    /*
    DAY, GTC
     */
    val timeInForce: String? = null,
    val limitPrice: Double? = null,
    val qty: Double? = null,
    val timestamp: Instant? = null,
    val date: LocalDate? = null,
    /*
    PENDING, ACTIVE, CANCELLED, EXECUTED, PARTIALLY_EXECUTED
     */
    val status: String? = null,
)