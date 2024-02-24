package com.example.tradingsystem

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.sql.DataSource

@Configuration
class TradingSystemConfiguration {
    
    @Bean
    fun dataSource(): DataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = System.getenv("JDBC_URL")
        hikariConfig.driverClassName = "com.mysql.cj.jdbc.Driver"
        hikariConfig.username = System.getenv("JDBC_USERNAME")
        hikariConfig.password = System.getenv("JDBC_PASSWORD")
        hikariConfig.maximumPoolSize = 10
        return HikariDataSource(hikariConfig)
    }
    
    @Bean
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }
    
    @Bean
    fun namedParameterJdbcTemplate(dataSource: DataSource): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(dataSource)
    }
    
}