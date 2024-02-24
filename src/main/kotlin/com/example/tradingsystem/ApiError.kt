package com.example.tradingsystem

data class ApiError(val message: String? = null, val validationErrors: List<ValidationError>? = null)