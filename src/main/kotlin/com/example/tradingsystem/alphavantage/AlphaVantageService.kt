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

    fun symbolSearch(keyword: String): MutableList<AlphaVantageMatch> {
        val url = "https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keyword=$keyword&apikey=$alphaVantageApiKey"
        val responseBody = executeGET(url)
        val json = objectMapper.readTree(responseBody)
        val bestMatches = json.get("bestMatches")
        if (bestMatches.isArray) {
            val matches = mutableListOf<AlphaVantageMatch>()
            for (node in bestMatches) {
                matches.add(
                    AlphaVantageMatch(
                        symbol = node.get("1. symbol").asText(),
                        name = node.get("2. name").asText(),
                        type = node.get("3. type").asText(),
                        region = node.get("3. region").asText(),
                        currency = node.get("3. currency").asText(),
                    )
                )
            }
            return matches
        } else {
            error("Unexpected response format") 
        }
    }
    
    fun getQuote(symbol: String): Double? {
        val url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=$symbol&apikey=$alphaVantageApiKey"
        val responseBody = executeGET(url)
        val json = objectMapper.readTree(responseBody)
        val price = json.at("/Global Quote/05. price").asDouble()
        return price
    }

    private fun executeGET(url: String): String? {
        val start = System.currentTimeMillis()
        log.info("Calling GET $url")
        val httpResponse = httpClient.send(
            HttpRequest.newBuilder().GET().uri(URI.create(url)).build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val responseBody = httpResponse.body()
        val stop = System.currentTimeMillis()
        log.info("Finished calling GET $url status=${httpResponse.statusCode()} body=$responseBody took ${stop - start}ms")
        return responseBody
    }
}