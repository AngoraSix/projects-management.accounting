package com.angorasix.projects.management.accounting.application

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.commons.infrastructure.intercommunication.projectmanagement.ManagementTasksClosed
import com.angorasix.projects.management.accounting.domain.accounting.aggregates.ContributorAccount
import com.angorasix.projects.management.accounting.domain.accounting.commands.ActivateAccountCommand
import com.angorasix.projects.management.accounting.domain.accounting.commands.AddTransactionCommand
import com.angorasix.projects.management.accounting.domain.accounting.commands.CreateContributorAccountCommand
import com.angorasix.projects.management.accounting.domain.accounting.entities.BalanceEffect
import com.angorasix.projects.management.accounting.domain.accounting.entities.DistributionType
import com.angorasix.projects.management.accounting.domain.accounting.entities.TimeBasedDistributionFactory
import com.angorasix.projects.management.accounting.domain.accounting.entities.Transaction
import com.angorasix.projects.management.accounting.domain.accounting.entities.TransactionOperation
import com.angorasix.projects.management.accounting.domain.accounting.entities.TransactionSource
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
import java.time.Duration
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
        ownershipCurrency: String?,
        // managedCurrencies: Set<String>, // not checked, future feature: Trello-CDwXGxpn
//        accountType: String, // You may convert this to an AccountType enum
//        initialBalance: Double,
//    ): CompletableFuture<String> {
    ): String =
        withContext(Dispatchers.Default) {
            ownershipCurrency?.let {
                val accountId = UUID.randomUUID().toString()
                val cmd =
                    CreateContributorAccountCommand(
                        accountId = accountId,
                        projectManagementId = projectManagementId,
                        contributorId = contributorId,
                        currency = it,
                        accountType = ContributorAccount.AccountType.OWNERSHIP,
                        createdInstant = Instant.now(),
                    )
                commandGateway.send<String>(cmd).await()
            }
            "return done"
//            return commandGateway.send(cmd)
        }

    suspend fun registerTaskEarnings(
        projectManagementId: String,
        closedTasks: List<ManagementTasksClosed.ManagementTaskClosed>,
        ownershipCurrency: String?,
        currencyDistributionRules: Map<String, ManagementTasksClosed.TasksDistributionRules>,
        // managedCurrencies: Set<String>, // not checked, future feature: Trello-CDwXGxpn
    ): String =
        withContext(Dispatchers.Default) {
            val batchTransactionId = UUID.randomUUID().toString()
            ownershipCurrency?.let { currency ->
                // 1) Group all closed tasks by each assignee (contributorId).
                //    Because a single task may have multiple assignees, we “flatten” into (contributorId → task) pairs.
                val perContributor: Map<String, List<ManagementTasksClosed.ManagementTaskClosed>> =
                    closedTasks
                        .filter {
                            it.caps != null && it.caps!! > 0.0
                        }.flatMap { task ->
                            task.assigneeContributorIds.map { contributorId ->
                                contributorId to task
                            }
                        }.groupBy({ it.first }, { it.second })
                // 2) For each contributor who has at least one closed task, look up their ownership account.
                //    If no active ownership account exists, skip that contributor.
                perContributor.forEach { (contributorId, tasksForThisContributor) ->
                    // Build a filter to find the contributor’s active ownership account in this projectManagementId:
                    val ownershipFilter =
                        ListAccountingFilter(
                            projectManagementId = setOf(projectManagementId),
                            accountStatus = setOf(ContributorAccount.ContributorAccountStatusValues.ACTIVE),
                            contributorId = setOf(contributorId),
                            accountType = setOf(ContributorAccount.AccountType.OWNERSHIP.name),
                            currency = setOf(currency),
                        )

                    // repository.findUsingFilter returns a Flow<ContributorAccountView>.
                    // We just want the first matching active ownership account (if any).
                    val ownershipAccountView: ContributorAccountView? =
                        repository
                            .findSingleUsingFilter(ownershipFilter, null)

                    if (ownershipAccountView == null) {
                        // No active ownership account found for this contributor; skip.
                        return@forEach
                    }

                    // 3) Build one TransactionOperation per closed task, using a triangular distribution:
                    val nowInstant = Instant.now()
                    val operations: List<TransactionOperation> =
                        tasksForThisContributor.map { task ->

                            // a) Determine the rule (duration) to use. For “startup”, always take startupDefaultDuration:
                            val rule =
                                currencyDistributionRules[currency]
                                    ?: throw IllegalArgumentException(
                                        "Missing distribution rules for currency '$currency'",
                                    )

                            // We want a full “triangle” whose total area = `caps`.
                            // Split the full duration into two halves:
                            val halfDuration: Duration = rule.startupDefaultDuration.dividedBy(2)

                            // (i) LINEAR_UP from doneInstant → doneInstant + halfDuration, area = caps
                            val up =
                                TimeBasedDistributionFactory.distributionForOwnership(
                                    distributionType = DistributionType.LINEAR_UP,
                                    mainValue = task.caps!!,
                                    startInstant = nowInstant, // or task.doneInstant?
                                    duration = halfDuration,
                                )

                            // (ii) LINEAR_DOWN from (doneInstant + halfDuration) → (doneInstant + fullDuration)
                            val down =
                                TimeBasedDistributionFactory.distributionForOwnership(
                                    distributionType = DistributionType.LINEAR_DOWN,
                                    mainValue = task.caps!!,
                                    startInstant = nowInstant.plus(halfDuration), // same, nowInstant or task.doneInstant?
                                    duration = halfDuration,
                                )

                            // Build a TransactionOperation that “credits” the contributor:
                            TransactionOperation(
                                balanceEffect = BalanceEffect.CREDIT,
                                valueDistribution = listOf(up, down),
                                fullyDefinedInstant = nowInstant,
                            )
                        }

                    // 4) Bundle all those operations into a single Transaction:
                    val transaction =
                        Transaction(
                            transactionId = batchTransactionId,
                            contributorAccountId = ownershipAccountView.accountId,
                            valueOperations = operations,
                            registeredInstant = nowInstant,
                            transactionSource =
                                TransactionSource(
                                    sourceType = "TASK_EARNINGS",
                                    sourceIds = tasksForThisContributor.map { it.taskId }.toSet(),
                                    sourceOperation = "closedTasksBatch",
                                ),
                        )

                    // 5) Send one AddTransactionCommand per‐contributor:
                    val addTxnCmd =
                        AddTransactionCommand(
                            accountId = ownershipAccountView.accountId,
                            transactionId = batchTransactionId,
                            transaction = transaction,
                        )
                    commandGateway.send<Any>(addTxnCmd).await() // wait for Axon to accept it
                }
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
