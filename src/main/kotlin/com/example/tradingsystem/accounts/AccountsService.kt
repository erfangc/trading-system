package com.example.tradingsystem.accounts

import com.example.tradingsystem.ValidationError
import com.example.tradingsystem.ValidationException
import com.example.tradingsystem.people.Address
import com.example.tradingsystem.people.Person
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

    fun getAccounts(): List<Account> {
        return namedParameterJdbcTemplate.jdbcTemplate.query(
            """
            select 
                account_number,
                account_type, 
                p.id,
                last_name, 
                middle_name, 
                first_name,
                login_id,
                birth_date,
                mailing_address_id,
                tax_id
            from account a, person p 
            where a.primary_owner_id = p.id
            """.trimIndent()
        ) { rs, _ ->
            Account(
                accountNumber = rs.getString("account_number"),
                accountType = rs.getString("account_type"),
                primaryOwner = Person(
                    id = rs.getInt("id"),
                    lastName = rs.getString("last_name"),
                    middleName = rs.getString("middle_name"),
                    firstName = rs.getString("first_name"),
                    loginId = rs.getString("login_id"),
                    birthDate = rs.getDate("birth_date").toLocalDate(),
                    taxId = rs.getString("tax_id"),
                ),
            )
        }
    }

    fun getAccount(accountNumber: String): Account {
        val account = namedParameterJdbcTemplate
            .queryForObject(
                """
                select 
                    a.account_number, account_type, primary_owner_id, 
                    p.id as person_id, last_name, middle_name, first_name, login_id, birth_date, mailing_address_id, tax_id,
                    m.id as mailing_address_id, line1, line2, city, state, zip_code
                from account a, person p, address m 
                where a.account_number = :account_number
                and a.primary_owner_id = p.id
                and p.mailing_address_id = m.id
                """.trimIndent(),
                mapOf("account_number" to accountNumber)
            ) { rs, _ ->
                Account(
                    accountNumber = rs.getString("account_number"),
                    accountType = rs.getString("account_type"),
                    primaryOwner = Person(
                        id = rs.getInt("person_id"),
                        lastName = rs.getString("last_name"),
                        middleName = rs.getString("middle_name"),
                        firstName = rs.getString("first_name"),
                        loginId = rs.getString("login_id"),
                        birthDate = rs.getDate("birth_date").toLocalDate(),
                        mailingAddress = Address(
                            id = rs.getInt("mailing_address_id"),
                            line1 = rs.getString("line1"),
                            line2 = rs.getString("line2"),
                            city = rs.getString("city"),
                            state = rs.getString("state"),
                            zip = rs.getString("zip_code"),
                        ),
                        taxId = rs.getString("tax_id"),
                    )
                )
            }
        val jointOwners = namedParameterJdbcTemplate.query(
            """
            select 
            p.id, last_name, middle_name, first_name, login_id, birth_date, mailing_address_id, tax_id,
            a.id as mailing_address_id, line1, line2, city, state, zip_code
            from joint_owners j, person p, address a
            where j.account_number = :account_number and j.owner_id = p.id and p.mailing_address_id = a.id    
            """.trimIndent(),
            mapOf("account_number" to accountNumber)
        ) { rs, _ ->
            Person(
                id = rs.getInt("id"),
                lastName = rs.getString("last_name"),
                middleName = rs.getString("middle_name"),
                firstName = rs.getString("first_name"),
                loginId = rs.getString("login_id"),
                birthDate = rs.getDate("birth_date").toLocalDate(),
                mailingAddress = Address(
                    id = rs.getInt("mailing_address_id"),
                    line1 = rs.getString("line1"),
                    line2 = rs.getString("line2"),
                    city = rs.getString("city"),
                    state = rs.getString("state"),
                    zip = rs.getString("zip_code"),
                ),
                taxId = rs.getString("tax_id"),
            )
        }
        
        return account?.copy(jointOwners = jointOwners)
            ?: throw ValidationException(validationErrors = listOf(ValidationError(message = "account $accountNumber cannot be found")))
    }

}