package com.example.tradingsystem.alphavantage

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("api/v1")
class AlphaVantageController(private val alphaVantageService: AlphaVantageService) {
    @GetMapping("assets")
    fun symbolSearch(keyword: String): MutableList<AlphaVantageMatch> {
        return alphaVantageService.symbolSearch(keyword)
    }
}