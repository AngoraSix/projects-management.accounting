package com.angorasix.projects.management.accounting.domain.accounting.commands

import com.angorasix.projects.management.accounting.domain.accounting.aggregates.ContributorAccount
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.Instant

data class CreateContributorAccountCommand(
    @TargetAggregateIdentifier
    val accountId: String,
    val projectManagementId: String,
    val contributorId: String,
    val currency: String,
    val accountType: ContributorAccount.AccountType,
    val createdInstant: Instant = Instant.now(),
)
