package com.example.tradingsystem

import com.example.tradingsystem.oms.OrderValidator

data class ApiError(val message: String? = null, val validationErrors: List<OrderValidator.ValidationError>? = null)