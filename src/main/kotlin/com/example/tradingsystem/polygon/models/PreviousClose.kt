package com.example.tradingsystem.polygon.models

data class PreviousClose(
    val ticker: String? = null,
    val adjusted: Boolean? = null,
    val results: List<PreviousCloseResult>? = null,
    val status: String? = null,
    val request_id: String? = null,
    val count: Int? = null,
)