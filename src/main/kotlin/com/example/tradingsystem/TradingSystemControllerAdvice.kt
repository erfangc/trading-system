package com.example.tradingsystem

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class TradingSystemControllerAdvice {
    
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(e: ValidationException): ResponseEntity<ApiError> {
        return ResponseEntity.badRequest().body(
            ApiError(
                validationErrors = e.validationErrors
            )
        )
    }
    
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(e: RuntimeException): ResponseEntity<ApiError> {
        e.printStackTrace()
        return ResponseEntity.internalServerError().body(
            ApiError(
                message = "A server error has occurred"
            )
        )
    }
    
}