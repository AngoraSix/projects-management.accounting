package com.angorasix.projects.management.accounting.domain.accounting.events

import com.angorasix.projects.management.accounting.domain.accounting.aggregates.ContributorAccount
import java.time.Instant

data class ContributorAccountCreatedEvent(
    val accountId: String,
    val projectManagementId: String,
    val contributorId: String,
    val currency: String,
    val accountType: ContributorAccount.AccountType,
    val createdInstant: Instant,
)
