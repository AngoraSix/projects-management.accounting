package com.angorasix.projects.management.accounting.domain.accounting.events

import java.time.Instant

data class AccountActivatedEvent(
    val accountId: String,
    val activationInstant: Instant,
)
