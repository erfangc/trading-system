package com.example.tradingsystem.people

import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

@Service
class PeopleService(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
    
    private val log = LoggerFactory.getLogger(PeopleService::class.java)
    private val jdbcTemplate = namedParameterJdbcTemplate.jdbcTemplate
    
    @Transactional
    fun createOrUpdate(input: Person): Person {
        /*
        Query by taxId, and loginId to see if this user already exists
        if they do just make updates if not then insert
         */
        val existing = queryExistingPerson(input)
        return if (existing != null) {
            runUpdateQuery(input, existing)
        } else {
            runInsertQuery(input)
        }
    }

    private fun runInsertQuery(input: Person): Person {
        val mailingAddress = input.mailingAddress
        namedParameterJdbcTemplate.update(
            """
            insert into address (line1, line2, city, state, zip_code) 
            values (:line1, :line2, :city, :state, :zip_code)
            """.trimIndent(),
            mapOf(
                "line1" to mailingAddress?.line1, 
                "line2" to mailingAddress?.line2, 
                "city" to mailingAddress?.city, 
                "state" to mailingAddress?.state, 
                "zip_code" to mailingAddress?.zip
            )
        )
        
        val mailingAddressId = jdbcTemplate.queryForObject("select last_insert_id() as id", Int::class.java)
        
        namedParameterJdbcTemplate.update(
            """
            insert into person (last_name, middle_name, first_name, login_id, birth_date, mailing_address_id, tax_id) 
            values (:last_name, :middle_name, :first_name, :login_id, :birth_date, :mailing_address_id, :tax_id)
            """.trimIndent(),
            mapOf(
                "last_name" to input.lastName,
                "middle_name" to input.middleName,
                "first_name" to input.firstName,
                "login_id" to input.loginId,
                "birth_date" to input.birthDate,
                "mailing_address_id" to mailingAddressId,
                "tax_id" to input.taxId,
            )
        )
        val id = jdbcTemplate.queryForObject("select last_insert_id() as id", Int::class.java)
        return input.copy(id = id, mailingAddress = input.mailingAddress?.copy(id = mailingAddressId))
    }

    private fun runUpdateQuery(input: Person, existing: Person): Person {
        log.info("Found existing person existing=$existing input=$input")

        val mailingAddress = input.mailingAddress
        if (mailingAddress != null) {
            namedParameterJdbcTemplate.update(
            """
            update address set line1 = :line1, line2 = :line2, city = :city, state = :state, zip_code = :zip_code where id = :id;
            """.trimIndent(),
                mapOf(
                    "id" to existing.mailingAddress?.id,
                    "line1" to mailingAddress.line1,
                    "line2" to mailingAddress.line2,
                    "city" to mailingAddress.city,
                    "state" to mailingAddress.state,
                    "zip_code" to mailingAddress.zip,
                )
            )
        }
        
        val ret = Person(
            id = existing.id,
            mailingAddress = mailingAddress, 
            lastName = input.lastName ?: existing.lastName, 
            middleName = input.middleName ?: existing.middleName, 
            firstName = input.firstName ?: existing.firstName, 
            loginId = input.loginId ?: existing.loginId, 
            birthDate = input.birthDate ?: existing.birthDate, 
            taxId = input.taxId ?: existing.taxId,
        )
        
        namedParameterJdbcTemplate.update(
            """
            update person set tax_id = :tax_id, login_id = :login_id, birth_date = :birth_date, first_name = :first_name, last_name = :last_name, middle_name = :middle_name where id = :id
            """.trimIndent(),
            mapOf(
                "id" to existing.id,
                "last_name" to ret.lastName,
                "middle_name" to ret.middleName,
                "first_name" to ret.firstName,
                "login_id" to ret.loginId,
                "birth_date" to ret.birthDate,
                "tax_id" to ret.taxId,
            )
        )
        
        return ret
    }

    private fun queryExistingPerson(input: Person) = getByLoginId(input.loginId) ?: getByTaxId(input.taxId)

    private val rowMapper = { rs: ResultSet, _: Int ->
        Person(
            id = rs.getInt("id"),
            lastName = rs.getString("last_name"),
            middleName = rs.getString("middle_name"),
            firstName = rs.getString("first_name"),
            loginId = rs.getString("login_id"),
            birthDate = rs.getDate("birth_date").toLocalDate(),
            taxId = rs.getString("tax_id"),
            mailingAddress = Address(id = rs.getInt("mailing_address_id"))
        )
    }

    fun getByLoginId(loginId: String?): Person? {
        if (loginId == null) {
            return null
        }
        return try {
            jdbcTemplate.queryForObject("select * from person where login_id = '$loginId'", rowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }
    
    fun getByTaxId(taxId: String?): Person? {
        if (taxId == null) {
            return null
        }
        return try {
            jdbcTemplate.queryForObject("select * from person where tax_id = '$taxId'", rowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }
    
}