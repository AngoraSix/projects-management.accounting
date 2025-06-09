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
import com.angorasix.projects.management.accounting.infrastructure.constants.TRANSACTION_SOURCES_TASK_EARNINGS
import com.angorasix.projects.management.accounting.infrastructure.domain.AccountStats
import com.angorasix.projects.management.accounting.infrastructure.domain.ContributorAccountingStats
import com.angorasix.projects.management.accounting.infrastructure.domain.ProjectAccountingStats
import com.angorasix.projects.management.accounting.infrastructure.domain.ProjectManagementAccountingStats
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.querymodel.ContributorAccountView
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.repository.ContributorAccountViewRepository
import com.angorasix.projects.management.accounting.infrastructure.queryfilters.ListAccountingFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.axonframework.commandhandling.gateway.CommandGateway
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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
    ): String =
        withContext(Dispatchers.Default) {
            ownershipCurrency
                ?.takeIf { it.isNotBlank() }
                ?.let { currency ->
                    groupByAssignee(closedTasks)
                        .forEach { (contributorId, tasks) ->
                            handleContributorEarnings(
                                projectManagementId,
                                contributorId,
                                tasks,
                                currency,
                                currencyDistributionRules,
                            )
                        }
                }

            "done"
        }

    private fun groupByAssignee(
        closed: List<ManagementTasksClosed.ManagementTaskClosed>,
    ): Map<String, List<ManagementTasksClosed.ManagementTaskClosed>> =
        closed
            .filter { it.assigneeContributorIds.isNotEmpty() && (it.caps ?: 0.0) > 0.0 }
            .flatMap { task ->
                task.assigneeContributorIds.map { it to task }
            }.groupBy({ it.first }, { it.second })

    private suspend fun handleContributorEarnings(
        projectManagementId: String,
        contributorId: String,
        tasks: List<ManagementTasksClosed.ManagementTaskClosed>,
        currency: String,
        rules: Map<String, ManagementTasksClosed.TasksDistributionRules>,
    ) {
        val transactionBatchId = UUID.randomUUID().toString()
        val ownership =
            repository.findSingleUsingFilter(
                ListAccountingFilter(
                    projectManagementId = setOf(projectManagementId),
                    accountStatus = setOf(ContributorAccount.ContributorAccountStatusValues.ACTIVE),
                    contributorId = setOf(contributorId),
                    accountType = setOf(ContributorAccount.AccountType.OWNERSHIP.name),
                    currency = setOf(currency),
                ),
                null,
            ) ?: return

        val newTasks =
            tasks.filterNot { t ->
                ownership.transactions
                    .filter { it.transactionSource.sourceType == TRANSACTION_SOURCES_TASK_EARNINGS }
                    .flatMap { it.transactionSource.sourceIds }
                    .contains(t.taskId)
            }
        if (newTasks.isEmpty()) return

        val now = Instant.now()
        val ops = newTasks.map { mkOwnershipOperation(it, now, rules[currency]!!) }
        val txn =
            Transaction(
                transactionId = transactionBatchId,
                contributorAccountId = ownership.accountId,
                valueOperations = ops,
                registeredInstant = now,
                transactionSource =
                    TransactionSource(
                        sourceType = TRANSACTION_SOURCES_TASK_EARNINGS,
                        sourceIds = newTasks.map { it.taskId }.toSet(),
                        sourceOperation = "closedTasksBatch",
                    ),
            )
        commandGateway
            .send<Any>(
                AddTransactionCommand(
                    transactionId = transactionBatchId,
                    accountId = ownership.accountId,
                    transaction = txn,
                ),
            ).await()
    }

    private fun mkOwnershipOperation(
        task: ManagementTasksClosed.ManagementTaskClosed,
        now: Instant,
        rule: ManagementTasksClosed.TasksDistributionRules,
    ): TransactionOperation {
        val half = rule.startupDefaultDuration.dividedBy(2)
        val upImpulse =
            TimeBasedDistributionFactory.distributionForOwnership(
                distributionType = DistributionType.IMPULSE,
                mainValue = task.caps!!,
                startInstant = now,
                duration = Duration.ZERO, // should be done automatically on Impulse definition anyway
            )
        val down =
            TimeBasedDistributionFactory.distributionForOwnership(
                distributionType = DistributionType.LINEAR_DOWN,
                mainValue = task.caps!!,
                startInstant = now.plus(half),
                duration = half,
            )
        return TransactionOperation(
            balanceEffect = BalanceEffect.CREDIT,
            valueDistribution = listOf(upImpulse, down),
            fullyDefinedInstant = now,
        )
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
    val (ownershipStats, financeStats) = this.filter { it.contributorId == contributorId }.toOwnershipVsFinancialStats()
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
private suspend fun Flow<ContributorAccountView>.toOwnershipVsFinancialStats(
    multipleCurrenciesAllowed: Boolean = true,
): Pair<AccountStats, List<AccountStats>> {
    // 1) Collect the Flow into a List
    val allAccounts: List<ContributorAccountView> = this.toList()

    // 2) Partition into “ownership” and “non-ownership” (i.e. financial or other types)
    val (ownershipAccounts, nonOwnershipAccounts) =
        allAccounts.partition { it.accountType == "OWNERSHIP" }

    // 3) Calculate ownership stats
    val requestInstant = Instant.now()
    val ownershipStats =
        if (ownershipAccounts.isNotEmpty() && (multipleCurrenciesAllowed || ownershipAccounts.size == 1)) {
            val currency = ownershipAccounts.first().currency
            val forecastedBalance =
                calculateForecastedBalance(
                    ownershipAccounts,
                    requestInstant,
                )
            AccountStats(balance = forecastedBalance.values.first(), currency = currency, forecastedBalance = forecastedBalance)
        } else {
            AccountStats(balance = 0.0, currency = "", forecastedBalance = emptyMap())
        }

    // 4) Group “nonOwnershipAccounts” by currency and calculate total balance per currency
    val financeStats: List<AccountStats> =
        nonOwnershipAccounts
            .groupBy { it.currency }
            .map { (currency, accountsOfSameCurrency) ->
                require(
                    multipleCurrenciesAllowed || accountsOfSameCurrency.size == 1,
                ) { "Expected only one account per currency, found ${accountsOfSameCurrency.size} for $currency" }

                val forecastedBalance =
                    calculateForecastedBalance(
                        accountsOfSameCurrency,
                        requestInstant,
                    )
                AccountStats(balance = forecastedBalance.values.first(), currency = currency, forecastedBalance = forecastedBalance)
            }
    return Pair(ownershipStats, financeStats)
}

const val ACCOUNTING_STATS_FORECASTED_MONTHS_INDEX_FIRST = 0
const val ACCOUNTING_STATS_FORECASTED_MONTHS_INDEX_LAST = 11
const val ACCOUNTING_STATS_FORECASTED_PERIOD_DAYS = 30L

private fun calculateForecastedBalance(
    ownershipAccounts: List<ContributorAccountView>,
    requestInstant: Instant,
): Map<String, Double> {
    // generate balance for each month from the requestInstant for 12 months,
    // using it.calculateBalanceAt(eachMonthInstant) and with map key MM-YYYY
    return ownershipAccounts
        .flatMap { account ->
            (ACCOUNTING_STATS_FORECASTED_MONTHS_INDEX_FIRST..ACCOUNTING_STATS_FORECASTED_MONTHS_INDEX_LAST).map { monthOffset ->
                val monthInstant = requestInstant.plus(Duration.ofDays(ACCOUNTING_STATS_FORECASTED_PERIOD_DAYS * monthOffset.toLong()))
                val balance = account.calculateBalanceAt(monthInstant)
                val formatter = DateTimeFormatter.ofPattern("MM-yyyy")
                val formatted = monthInstant.atZone(ZoneOffset.UTC).format(formatter)
                formatted to balance
            }
        }.groupBy({ it.first }, { it.second })
        .mapValues { (_, balances) -> balances.sum() } // Sum balances for each month
}
