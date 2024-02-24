package com.example.tradingsystem

import com.example.tradingsystem.oms.OrderValidator

class ValidationException(val validationErrors: List<OrderValidator.ValidationError>) : RuntimeException()