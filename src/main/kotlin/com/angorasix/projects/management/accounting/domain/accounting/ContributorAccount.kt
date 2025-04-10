package com.angorasix.projects.management.accounting.domain.accounting

import com.angorasix.projects.management.accounting.domain.accounting.commands.ActivateAccountCommand
import com.angorasix.projects.management.accounting.domain.accounting.commands.AddTransactionCommand
import com.angorasix.projects.management.accounting.domain.accounting.commands.CreateContributorAccountCommand
import com.angorasix.projects.management.accounting.domain.accounting.events.AccountActivatedEvent
import com.angorasix.projects.management.accounting.domain.accounting.events.ContributorAccountCreatedEvent
import com.angorasix.projects.management.accounting.domain.accounting.events.TransactionAddedEvent
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate
import java.time.Instant

@Aggregate
class ContributorAccount() {
    @AggregateIdentifier
    private lateinit var accountId: String

    private var contributorId: String? = null
    private var currency: String? = null
    private var accountType: AccountType? = null
    private var status: ContributorAccountStatus =
        ContributorAccountStatus(
            status = ContributorAccountStatusValues.PENDING,
        )
    private val transactions: MutableList<Transaction> = mutableListOf()

    @CommandHandler
    constructor(cmd: CreateContributorAccountCommand) : this() {
        apply(
            ContributorAccountCreatedEvent(
                accountId = cmd.accountId,
                projectManagementId = cmd.projectManagementId,
                contributorId = cmd.contributorId,
                currency = cmd.currency,
                accountType = cmd.accountType,
                createdInstant = cmd.createdInstant,
            ),
        )
    }

    @CommandHandler
    fun handle(cmd: AddTransactionCommand) {
        apply(
            TransactionAddedEvent(
                accountId = cmd.accountId,
                transactionId = cmd.transactionId,
                transaction = cmd.transaction,
            ),
        )
    }

    @CommandHandler
    fun handle(cmd: ActivateAccountCommand) {
        if (status.status == ContributorAccountStatusValues.PENDING) {
            apply(AccountActivatedEvent(cmd.accountId, cmd.activationInstant))
        }
    }

    @EventSourcingHandler
    fun on(event: ContributorAccountCreatedEvent) {
        accountId = event.accountId
        contributorId = event.contributorId
        currency = event.currency
        accountType = event.accountType
        status = ContributorAccountStatus(ContributorAccountStatusValues.ACTIVE, event.createdInstant)
    }

    @EventSourcingHandler
    fun on(event: TransactionAddedEvent) {
        transactions.add(event.transaction)
        // Optionally, update additional state if needed.
    }

    @EventSourcingHandler
    fun on(event: AccountActivatedEvent) {
        status =
            status.copy(
                activationDate = event.activationInstant,
                status = ContributorAccountStatusValues.ACTIVE,
            )
    }

    // Business method to compute the current balance.
    fun currentBalance(): Double = transactions.sumOf { tx -> tx.valueOperations.sumOf { op -> op.valueDistribution.integrateToNow() } }
}

data class ContributorAccountStatus(
    val status: ContributorAccountStatusValues,
    val activationDate: Instant? = null, // the account activationDate
)

enum class ContributorAccountStatusValues {
    PENDING,
    ACTIVE,
    DISABLED,
}

enum class AccountType {
    OWNERSHIP,
    FINANCIAL,
}
