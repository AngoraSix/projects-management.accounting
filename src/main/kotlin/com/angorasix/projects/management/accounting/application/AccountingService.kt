package com.angorasix.projects.management.accounting.application

import com.angorasix.commons.domain.projectmanagement.accounting.A6_OWNERSHIP_CAPS_CURRENCY_ID
import com.angorasix.projects.management.accounting.domain.accounting.aggregates.AccountType
import com.angorasix.projects.management.accounting.domain.accounting.commands.ActivateAccountCommand
import com.angorasix.projects.management.accounting.domain.accounting.commands.AddTransactionCommand
import com.angorasix.projects.management.accounting.domain.accounting.commands.CreateContributorAccountCommand
import com.angorasix.projects.management.accounting.domain.accounting.entities.Transaction
import kotlinx.coroutines.Dispatchers
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
    private val commandGateway: CommandGateway,
//    private val repository: IntegrationAssetRepository,
//    private val streamBridge: StreamBridge,
//    private val amqpConfigs: AmqpConfigurations,
) {
    suspend fun createContributorAccountsForProjectManagement(
        projectManagementId: String,
        contributorId: String,
        requiresOwnershipAccount: Boolean,
//        managedCurrencies: List<String>, Future feature: Trello-CDwXGxpn
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
                        accountType = AccountType.OWNERSHIP,
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
}

// WOULd I NEED THIs If THIS DOESN'T WORK?
// <dependency>
// <groupId>org.jetbrains.kotlinx</groupId>
// <artifactId>kotlinx-coroutines-jdk8</artifactId>
// <version>1.7.1</version> <!-- or the version you use -->
// </dependency>
