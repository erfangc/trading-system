package com.example.tradingsystem

class ValidationException(val validationErrors: List<ValidationError>) : RuntimeException()