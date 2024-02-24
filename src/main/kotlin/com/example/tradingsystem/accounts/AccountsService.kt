package com.example.tradingsystem.accounts

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountsService(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {

    private val log = LoggerFactory.getLogger(AccountsService::class.java)
    private val jdbcTemplate = namedParameterJdbcTemplate.jdbcTemplate

    @Transactional
    fun createAccount(req: CreateAccountRequest): Account {
        jdbcTemplate.update(
            """
            update next_account_number
            set next_account_number = next_account_number + 1
        """.trimIndent()
        )

        val accountNumber = jdbcTemplate
            .queryForObject(
                """
                select next_account_number
                from next_account_number
                """.trimIndent(),
                Int::class.java
            ).toString().padStart(12, '0')

        log.info("Creating account accountNumber=$accountNumber for primaryOwner=${req.primaryOwner}")
        namedParameterJdbcTemplate.update(
            """
            insert into account (account_number, account_type, primary_owner_id) values (:account_number, :account_type, :primary_owner_id)
            """.trimIndent(),
            mapOf(
                "account_number" to accountNumber,
                "account_type" to req.accountType,
                "primary_owner_id" to req.primaryOwner?.id
            )
        )

        req.jointOwners?.let { jointOwners ->
            for (jointOwner in jointOwners) {
                log.info("Setting joint owner accountNumber=$accountNumber for jointOwner=$jointOwner")
                namedParameterJdbcTemplate.update(
                    """
                    insert into joint_owners (account_number, owner_id) values (:account_number, :owner_id);
                    """.trimIndent(),
                    mapOf(
                        "account_number" to accountNumber,
                        "owner_id" to jointOwner.id,
                    )
                )
            }
        }

        return Account(
            accountNumber = accountNumber,
            accountType = req.accountType,
            primaryOwner = req.primaryOwner,
            jointOwners = req.jointOwners,
        )
    }

}