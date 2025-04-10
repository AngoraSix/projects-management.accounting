package com.angorasix.projects.management.accounting.infrastructure.eventsourcing.projections

import com.angorasix.projects.management.accounting.domain.accounting.ContributorAccountStatusValues
import com.angorasix.projects.management.accounting.domain.accounting.events.AccountActivatedEvent
import com.angorasix.projects.management.accounting.domain.accounting.events.ContributorAccountCreatedEvent
import com.angorasix.projects.management.accounting.domain.accounting.events.TransactionAddedEvent
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.querymodel.ContributorAccountStatusView
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.querymodel.ContributorAccountView
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.repository.ContributorAccountViewRepository
import kotlinx.coroutines.runBlocking
import org.axonframework.eventhandling.EventHandler
import java.time.Instant

class ContributorAccountProjection(
    private val repository: ContributorAccountViewRepository,
) {
    //    suspend fun on(event: ContributorAccountCreatedEvent) {
    @EventHandler
    fun on(event: ContributorAccountCreatedEvent) {
        runBlocking {
            val view =
                ContributorAccountView(
                    accountId = event.accountId,
                    projectManagementId = event.projectManagementId,
                    contributorId = event.contributorId,
                    currency = event.currency,
                    accountType = event.accountType.name,
                    lastUpdatedInstant = event.createdInstant,
                    lastUpdatedBalance = 0.0,
                    unprocessedTransactionOperations = mutableListOf(),
                    status =
                        ContributorAccountStatusView(
                            status = ContributorAccountStatusValues.PENDING,
                        ),
                )
            repository.save(view)
        }
    }

    @EventHandler
    fun on(event: TransactionAddedEvent) {
        runBlocking {
            repository
                .findById(event.accountId)
                ?.let { view ->
                    // Here you might recalculate the new balance using your domain logic;
                    // for demonstration, we add the integrated value from the transaction.
                    val additional = event.transaction.valueOperations.sumOf { op -> op.valueDistribution.integrateToNow() }
                    val updated =
                        view.copy(
                            lastUpdatedBalance = view.lastUpdatedBalance + additional,
                            lastUpdatedInstant = Instant.now(),
                        )
                    repository.save(updated)
                }
        }
    }

    @EventHandler
    fun on(event: AccountActivatedEvent) {
        runBlocking {
            repository
                .findById(event.accountId)
                ?.let { view ->
                    // USE CREATIONAL PATTERN HERE TO AVOID COPYING TWICE
                    val updated =
                        view.copyUpdateToInstant(event.activationInstant).copy(
                            status =
                                ContributorAccountStatusView(
                                    status = ContributorAccountStatusValues.ACTIVE,
                                    activationDate = event.activationInstant,
                                ),
                        )
                    repository.save(updated)
                }
        }
    }
}
