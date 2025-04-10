package com.angorasix.projects.management.accounting.domain.accounting.commands

import java.time.Instant

data class ActivateAccountCommand(
    val accountId: String,
    val activationInstant: Instant = Instant.now(),
)
