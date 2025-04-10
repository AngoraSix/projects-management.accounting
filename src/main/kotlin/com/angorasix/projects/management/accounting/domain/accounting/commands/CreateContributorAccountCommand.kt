package com.angorasix.projects.management.accounting.domain.accounting.commands

import com.angorasix.projects.management.accounting.domain.accounting.AccountType
import java.time.Instant

data class CreateContributorAccountCommand(
    val accountId: String,
    val projectManagementId: String,
    val contributorId: String,
    val currency: String,
    val accountType: AccountType,
    val createdInstant: Instant = Instant.now(),
)
