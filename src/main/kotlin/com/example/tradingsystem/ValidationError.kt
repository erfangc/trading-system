package com.example.tradingsystem

data class ValidationError(
    val code: String? = null,
    val message: String? = null,
    val field: String? = null,
)