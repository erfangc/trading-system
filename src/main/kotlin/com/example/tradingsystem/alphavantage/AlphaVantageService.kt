package com.example.tradingsystem.alphavantage

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class AlphaVantageService(private val objectMapper: ObjectMapper) {

    private val alphaVantageApiKey = System.getenv("ALPHA_VANTAGE_API_KEY")
    private val log = LoggerFactory.getLogger(AlphaVantageService::class.java)
    private val httpClient = HttpClient.newHttpClient()
    
    fun getQuote(symbol: String): Double? {
        val url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=$symbol&apikey=$alphaVantageApiKey"
        val start = System.currentTimeMillis()
        log.info("Calling GET $url")
        val httpResponse = httpClient.send(
            HttpRequest.newBuilder().GET().uri(URI.create(url)).build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val responseBody = httpResponse.body()
        val stop = System.currentTimeMillis()
        log.info("Finished calling GET $url status=${httpResponse.statusCode()} body=$responseBody took ${stop - start}ms")
        val json = objectMapper.readTree(responseBody)
        val price = json.at("/Global Quote/05. price").asDouble()
        return price
    }
}