package com.example.tradingsystem.people

import java.time.LocalDate

data class Person(
    val id: Int? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val firstName: String? = null,
    val loginId: String? = null,
    val birthDate: LocalDate? = null,
    val mailingAddress: Address? = null,
    val taxId: String? = null,
)
