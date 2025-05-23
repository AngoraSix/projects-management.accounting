package com.angorasix.projects.management.accounting.domain.accounting.events

import com.angorasix.projects.management.accounting.domain.accounting.entities.Transaction

data class TransactionAddedEvent(
    val accountId: String,
    val transactionId: String,
    val transaction: Transaction,
)
