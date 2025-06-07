package com.angorasix.projects.management.accounting.messaging.handler

import com.angorasix.commons.infrastructure.intercommunication.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.messaging.A6InfraMessageDto
import com.angorasix.commons.infrastructure.intercommunication.projectmanagement.ManagementTasksClosed
import com.angorasix.commons.infrastructure.intercommunication.projectmanagement.ProjectManagementContributorRegistered
import com.angorasix.projects.management.accounting.application.AccountingService
import kotlinx.coroutines.runBlocking

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
class AccountingMessagingHandler(
    private val service: AccountingService,
) {
    fun createContributorAccountsForMgmt(message: A6InfraMessageDto<ProjectManagementContributorRegistered>) =
        runBlocking {
            if (message.topic == A6InfraTopics.PROJECT_MANAGEMENT_CONTRIBUTOR_REGISTERED.value &&
                message.targetType == A6DomainResource.PROJECT_MANAGEMENT
            ) {
                val contributorRegisteredEvent = message.messageData

                service.createContributorAccountsForProjectManagement(
                    projectManagementId = contributorRegisteredEvent.projectManagementId,
                    contributorId = contributorRegisteredEvent.registeredContributorId,
                    ownershipCurrency = contributorRegisteredEvent.ownershipCurrency,
//                    managedCurrencies = contributorRegisteredEvent.managementFinancialCurrencies,
                )
            }
        }

    fun registerTaskEarnings(message: A6InfraMessageDto<ManagementTasksClosed>) =
        runBlocking {
            if (message.topic == A6InfraTopics.PROJECT_MANAGEMENT_TASKS_CLOSED.value &&
                message.targetType == A6DomainResource.PROJECT_MANAGEMENT
            ) {
                val tasksClosedEvent = message.messageData

                service.registerTaskEarnings(
                    projectManagementId = tasksClosedEvent.projectManagementId,
                    closedTasks = tasksClosedEvent.collection,
                    ownershipCurrency = tasksClosedEvent.ownershipCurrency,
                    currencyDistributionRules = tasksClosedEvent.currencyDistributionRules,
//                    managedCurrencies = contributorRegisteredEvent.managementFinancialCurrencies,
                )
            }
        }
}
