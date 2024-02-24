package com.example.tradingsystem.accounts

import com.example.tradingsystem.people.Person

data class CreateAccountRequest(
    val accountType: String,
    val primaryOwner: Person? = null,
    val jointOwners: List<Person>? = null
)