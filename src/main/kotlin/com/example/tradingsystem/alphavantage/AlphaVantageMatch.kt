package com.example.tradingsystem.alphavantage

// Define the POJO class to represent the data you want
data class AlphaVantageMatch(
    val symbol: String,
    val name: String,
    val type: String,
    val region: String,
    val currency: String,
)