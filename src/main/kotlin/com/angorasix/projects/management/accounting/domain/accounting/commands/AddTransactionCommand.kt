package com.angorasix.projects.management.accounting.domain.accounting.commands

import com.angorasix.projects.management.accounting.domain.accounting.Transaction
import java.time.Instant

data class AddTransactionCommand(
    val accountId: String,
    val transactionId: String,
    val transaction: Transaction,
    val createdInstant: Instant = Instant.now(),
)
