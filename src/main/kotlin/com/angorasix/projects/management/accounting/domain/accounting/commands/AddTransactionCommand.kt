package com.angorasix.projects.management.accounting.domain.accounting.commands

import com.angorasix.projects.management.accounting.domain.accounting.entities.Transaction
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.Instant

data class AddTransactionCommand(
    @TargetAggregateIdentifier
    val accountId: String,
    val transactionId: String,
    val transaction: Transaction,
    val createdInstant: Instant = Instant.now(),
)
