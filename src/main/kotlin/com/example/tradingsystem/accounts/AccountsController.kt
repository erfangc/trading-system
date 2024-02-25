package com.example.tradingsystem.accounts

import com.example.tradingsystem.people.PeopleService
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1")
class AccountsController(
    private val peopleService: PeopleService,
    private val accountsService: AccountsService,
) {

    private val log = LoggerFactory.getLogger(AccountsController::class.java)

    @Transactional
    @PostMapping("accounts")
    fun createAccount(@RequestBody createAccountRequest: CreateAccountRequest): Account {
        log.info("Creating primary owner ${createAccountRequest.primaryOwner}")
        val primaryOwner = createAccountRequest.primaryOwner?.let { peopleService.createOrUpdate(it) }
        log.info("Creating joint owners ${createAccountRequest.jointOwners}")
        val jointOwners = createAccountRequest.jointOwners?.map { jointOwner -> peopleService.createOrUpdate(jointOwner) }
        log.info("Creating account accountType=${createAccountRequest.accountType}")
        return accountsService.createAccount(createAccountRequest.copy(primaryOwner = primaryOwner, jointOwners = jointOwners))
    }


    @GetMapping("accounts")
    fun getAccounts(): List<Account> {
        return accountsService.getAccounts()
    }

    @GetMapping("account/{accountNumber}")
    fun getAccount(@PathVariable accountNumber: String): List<Account> {
        TODO()
    }

}