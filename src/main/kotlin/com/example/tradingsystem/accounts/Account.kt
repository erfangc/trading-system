package com.example.tradingsystem.accounts

import com.example.tradingsystem.people.Person

data class Account(
    val accountNumber: String? = null,
    val accountType: String? = null,
    val primaryOwner: Person? = null,
    val jointOwners: List<Person>? = null,
)

