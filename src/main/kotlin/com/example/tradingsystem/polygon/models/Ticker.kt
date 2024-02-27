package com.example.tradingsystem.polygon.models

data class Ticker(
    val ticker: String? = null,
    val todaysChangePerc: Double? = null,
    val todaysChange: Double? = null,
    val updated: Long? = null,
    val day: Day? = null,
    val min: Min? = null,
    val prevDay: PrevDay? = null,
)