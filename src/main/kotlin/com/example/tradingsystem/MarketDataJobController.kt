package com.example.tradingsystem

import com.example.tradingsystem.jobs.RunDailyPriceJobResult
import com.example.tradingsystem.polygon.PolygonService
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.concurrent.Executors

@RestController
@RequestMapping("api/v1")
class MarketDataJobController(
    private val polygonService: PolygonService,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    
    private val executor = Executors.newSingleThreadExecutor()
    private val log = LoggerFactory.getLogger(MarketDataJobController::class.java)

    @PostMapping("daily-price-job-runs")
    fun runDailyPriceJob(): RunDailyPriceJobResult {
        executor.execute { 
            val allTickers = polygonService.allTickers()
            allTickers.tickers?.let { tickers ->
                val args = tickers.distinctBy { it.ticker?.uppercase() }.map { ticker ->
                    arrayOf(
                        ticker.ticker,
                        ticker.todaysChangePerc,
                        ticker.todaysChange,
                        ticker.updated?.let { nano ->
                            val seconds = nano / 1_000_000_000L
                            val nanos = nano % 1_000_000_000L
                            Instant.ofEpochSecond(seconds, nanos)
                        },
                        ticker.day?.c,
                        ticker.day?.h,
                        ticker.day?.l,
                        ticker.day?.o,
                        ticker.day?.v,
                        ticker.todaysChangePerc,
                        ticker.todaysChange,
                        ticker.updated?.let { nano ->
                            val seconds = nano / 1_000_000_000L
                            val nanos = nano % 1_000_000_000L
                            Instant.ofEpochSecond(seconds, nanos)
                        },
                        ticker.day?.c,
                        ticker.day?.h,
                        ticker.day?.l,
                        ticker.day?.o,
                        ticker.day?.v,
                    )
                }
                val chunks = args.chunked(500)
                for ((idx, chunk) in chunks.withIndex()) {
                    log.info("Inserting ${chunk.size} entries as a bulk upload into prices table chunk ${idx + 1}/${chunks.size}")
                    val start = System.currentTimeMillis()
                    namedParameterJdbcTemplate.jdbcTemplate.batchUpdate(
                        """
                        insert into prices (ticker, daily_change_percent, daily_change, updated, close_price, high_price, low_price, open_price, volume) 
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?) on duplicate key update daily_change_percent = ?, daily_change = ?, updated = ?, close_price = ?, high_price = ?, low_price = ?, open_price = ?, volume = ?
                    """.trimIndent(),
                        chunk
                    )
                    val stop = System.currentTimeMillis()
                    log.info("Inserted ${chunk.size} entries as a bulk upload into prices table took ${stop - start}ms ${idx + 1}/${chunks.size}")   
                }
            }
            log.info("Daily price job finished")
        }
        return RunDailyPriceJobResult()
    }
}
