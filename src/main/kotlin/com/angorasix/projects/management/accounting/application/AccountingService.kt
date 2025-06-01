package com.angorasix.projects.management.accounting.application

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.commons.domain.projectmanagement.accounting.A6_OWNERSHIP_CAPS_CURRENCY_ID
import com.angorasix.projects.management.accounting.domain.accounting.aggregates.ContributorAccount
import com.angorasix.projects.management.accounting.domain.accounting.commands.ActivateAccountCommand
import com.angorasix.projects.management.accounting.domain.accounting.commands.AddTransactionCommand
import com.angorasix.projects.management.accounting.domain.accounting.commands.CreateContributorAccountCommand
import com.angorasix.projects.management.accounting.domain.accounting.entities.Transaction
import com.angorasix.projects.management.accounting.infrastructure.domain.AccountStats
import com.angorasix.projects.management.accounting.infrastructure.domain.ContributorAccountingStats
import com.angorasix.projects.management.accounting.infrastructure.domain.ProjectAccountingStats
import com.angorasix.projects.management.accounting.infrastructure.domain.ProjectManagementAccountingStats
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.querymodel.ContributorAccountView
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.repository.ContributorAccountViewRepository
import com.angorasix.projects.management.accounting.infrastructure.queryfilters.ListAccountingFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.axonframework.commandhandling.gateway.CommandGateway
import java.time.Instant
import java.util.UUID

/**
 *
 *
 * @author rozagerardo
 */
class AccountingService(
    private val repository: ContributorAccountViewRepository,
    private val commandGateway: CommandGateway,
) {
    suspend fun createContributorAccountsForProjectManagement(
        projectManagementId: String,
        contributorId: String,
        requiresOwnershipAccount: Boolean,
        // managedCurrencies: Set<String>, // not checked, future feature: Trello-CDwXGxpn
//        accountType: String, // You may convert this to an AccountType enum
//        initialBalance: Double,
//    ): CompletableFuture<String> {
    ): String =
        withContext(Dispatchers.Default) {
            if (requiresOwnershipAccount) {
                val accountId = UUID.randomUUID().toString()
                val cmd =
                    CreateContributorAccountCommand(
                        accountId = accountId,
                        projectManagementId = projectManagementId,
                        contributorId = contributorId,
                        currency = A6_OWNERSHIP_CAPS_CURRENCY_ID,
                        accountType = ContributorAccount.AccountType.OWNERSHIP,
                        createdInstant = Instant.now(),
                    )
                commandGateway.send<String>(cmd).await()
            }
            "return done"
//            return commandGateway.send(cmd)
        }

    suspend fun addTransaction(
        accountId: String,
        transaction: Transaction,
//    ): CompletableFuture<Void> {
    ): Void =
        withContext(Dispatchers.Default) {
            val transactionId = UUID.randomUUID().toString()
            val cmd =
                AddTransactionCommand(
                    accountId = accountId,
                    transactionId = transactionId,
                    transaction = transaction,
                    createdInstant = Instant.now(),
                )
            commandGateway.send<Void>(cmd).await()
        }

    suspend fun activateAccount(accountId: String): Void =
//            CompletableFuture<Void> { =
        withContext(Dispatchers.Default) {
            val cmd =
                ActivateAccountCommand(
                    accountId = accountId,
                    activationInstant = Instant.now(),
                )
//        return commandGateway.send(cmd)
            commandGateway.send<Void>(cmd).await()
        }

    suspend fun resolveProjectManagementTasksStats(
        projectManagementId: String,
        requestingContributor: A6Contributor?,
    ): ProjectManagementAccountingStats {
        val contributorAccounts =
            requestingContributor?.let {
                repository.findUsingFilter(
                    ListAccountingFilter(
                        projectManagementId = setOf(projectManagementId),
                        accountStatus =
                            setOf(
                                ContributorAccount.ContributorAccountStatusValues.ACTIVE,
                                ContributorAccount.ContributorAccountStatusValues.PENDING,
                            ),
                        contributorId = setOf(it.contributorId),
                    ),
                    it,
                )
            }

        val projectManagementAccounts =
            repository.findUsingFilter(
                ListAccountingFilter(
                    projectManagementId = setOf(projectManagementId),
                    accountStatus =
                        setOf(
                            ContributorAccount.ContributorAccountStatusValues.ACTIVE,
                            ContributorAccount.ContributorAccountStatusValues.PENDING,
                        ),
                ),
                null,
            )

        return ProjectManagementAccountingStats(
            projectManagementId = projectManagementId,
            project = projectManagementAccounts.toProjectStats(),
            contributor = contributorAccounts?.toContributorStats(requestingContributor.contributorId),
        )
    }
}

// WOULd I NEED THIs If THIS DOESN'T WORK?
// <dependency>
// <groupId>org.jetbrains.kotlinx</groupId>
// <artifactId>kotlinx-coroutines-jdk8</artifactId>
// <version>1.7.1</version> <!-- or the version you use -->
// </dependency>

suspend fun Flow<ContributorAccountView>.toProjectStats(): ProjectAccountingStats {
    val (ownershipStats, financeStats: List<AccountStats>) = toOwnershipVsFinancialStats()
    return ProjectAccountingStats(
        ownership = ownershipStats,
        finance = financeStats,
    )
}

suspend fun Flow<ContributorAccountView>.toContributorStats(contributorId: String): ContributorAccountingStats {
    val (ownershipStats, financeStats: List<AccountStats>) = toOwnershipVsFinancialStats()
    return ContributorAccountingStats(
        contributorId = contributorId,
        ownership = ownershipStats,
        finance = financeStats,
    )
}

/**
 * Flow<ContributorAccountView> extension that collects the accounts,
 * partitions them between “ownership” vs “financial” and retrieves an AccountingStats.
 */
private suspend fun Flow<ContributorAccountView>.toOwnershipVsFinancialStats(): Pair<AccountStats, List<AccountStats>> {
    // 1) Collect the Flow into a List
    val allAccounts: List<ContributorAccountView> = this.toList()

    // 2) Partition into “ownership” and “non-ownership” (i.e. financial or other types)
    val (ownershipAccounts, nonOwnershipAccounts) =
        allAccounts.partition { it.accountType == "OWNERSHIP" }

    // 3) Calculate ownership stats
    val ownershipStats =
        if (ownershipAccounts.isNotEmpty()) {
            val currency = ownershipAccounts.first().currency
            val totalBalance = ownershipAccounts.sumOf { it.calculateCurrentBalance() }
            AccountStats(balance = totalBalance, currency = currency)
        } else {
            AccountStats(balance = 0.0, currency = "")
        }

    // 4) Group “nonOwnershipAccounts” by currency and calculate total balance per currency
    val financeStats: List<AccountStats> =
        nonOwnershipAccounts
            .groupBy { it.currency }
            .map { (currency, accountsOfSameCurrency) ->
                val totalBalanceForThatCurrency = accountsOfSameCurrency.sumOf { it.calculateCurrentBalance() }
                AccountStats(balance = totalBalanceForThatCurrency, currency = currency)
            }
    return Pair(ownershipStats, financeStats)
}
