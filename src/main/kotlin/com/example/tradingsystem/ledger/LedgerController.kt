package com.example.tradingsystem.ledger

import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/")
class LedgerController(private val ledgerService: LedgerService) {

    @PostMapping("transactions")
    fun createTransaction(
        @RequestParam(required = false) accountNumber: String? = null,
        @RequestParam(required = false) contraAccountNumber: String? = null,
        @RequestParam(required = false) securityId: String? = null,
        @RequestParam(required = false) qty: Double? = null,
        @RequestParam(required = false) price: Double? = null,
        @RequestParam(required = false) cashImpact: Double? = null,
        @RequestParam(required = false) effectiveDate: LocalDate? = null,
        @RequestParam(required = false) settleDate: LocalDate? = null,
        @RequestParam(required = false) memo: String? = null,
    ): Transaction {
        return ledgerService.createTransaction(
            Transaction(
                accountNumber = accountNumber,
                contraAccountNumber = contraAccountNumber,
                securityId = securityId,
                qty = qty,
                price = price,
                cashImpact = cashImpact,
                effectiveDate = effectiveDate,
                settleDate = settleDate,
                memo = memo,
            )
        )
    }

    @PostMapping("cash-transactions")
    fun createCashTransaction(accountNumber: String, amount: Double) {
        return ledgerService.createCashTransaction(accountNumber, amount)
    }

    @GetMapping("accounts/{accountNumber}/transactions")
    fun getTransactions(@PathVariable accountNumber: String): List<Transaction> {
        return ledgerService.getTransactions(accountNumber)
    }

    @GetMapping("accounts/{accountNumber}/positions")
    fun getPositions(@PathVariable accountNumber: String): List<Position> {
        return ledgerService.getPositions(accountNumber)
    }

}