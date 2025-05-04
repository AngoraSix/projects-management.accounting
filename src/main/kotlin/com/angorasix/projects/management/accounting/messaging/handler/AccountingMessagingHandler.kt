package com.angorasix.projects.management.accounting.messaging.handler

import com.angorasix.commons.infrastructure.intercommunication.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.messaging.A6InfraMessageDto
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
                    requiresOwnershipAccount =
                        contributorRegisteredEvent.participatesInOwnership,
//                    managedCurrencies = contributorRegisteredEvent.managementFinancialCurrencies,
                )
            }
        }
}
