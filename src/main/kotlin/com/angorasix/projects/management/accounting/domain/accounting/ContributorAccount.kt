package com.angorasix.projects.management.accounting.domain.accounting

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.spring.stereotype.Aggregate
import java.time.Instant

@Aggregate
class ContributorAccount() {
    @AggregateIdentifier
    private lateinit var accountId: String

    private var currency: String? = null

    private var accountType: AccountType? = null

    private var status: ContributorAccountStatus =
        ContributorAccountStatus(
            status = ContributorAccountStatusValues.PENDING,
        )

//    private var balance
//    might need it to enforce rules, but might want to make it more performant

    @CommandHandler
    constructor(cmd: CreateContributorAccountCommand) : this() {
        apply(ContributorAccountCreatedEvent(cmd.accountId, cmd.currency, cmd.accountType))
    }

    @CommandHandler
    fun handle(cmd: AddTransactionCommand) {
        // domain validations
        require(!cmd.functions.isNullOrEmpty()) { "At least one distribution function is required." }
        apply(TransactionAddedEvent(accountId, cmd.transactionId, cmd.functions, cmd.timestamp))
    }

    @EventSourcingHandler
    fun on(evt: ContributorAccountCreatedEvent) {
        this.accountId = evt.accountId
        this.currency = evt.currency
        this.accountType = evt.accountType
        // other initialization logic
    }

    @EventSourcingHandler
    fun on(evt: TransactionAddedEvent) {
        // possibly store transaction references or essential domain data
        // e.g., a mutable list of transactions, if you want to maintain them in the aggregate
    }
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
