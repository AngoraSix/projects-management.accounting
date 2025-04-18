package com.angorasix.projects.management.accounting.messaging.handler

import com.angorasix.commons.domain.projectmanagement.core.BylawWellknownScope
import com.angorasix.commons.infrastructure.intercommunication.dto.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.dto.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.dto.messaging.A6InfraMessageDto
import com.angorasix.commons.infrastructure.intercommunication.dto.projectmanagement.ProjectManagementContributorRegistered
import com.angorasix.projects.management.accounting.application.AccountingService
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
class AccountingMessagingHandler(
    private val service: AccountingService,
    private val objectMapper: ObjectMapper,
) {
    fun createContributorAccountsForMgmt(message: A6InfraMessageDto) =
        runBlocking {
            // TBD all this
            if (message.topic == A6InfraTopics.PROJECT_MANAGEMENT_CONTRIBUTOR_REGISTERED.value &&
                message.targetType == A6DomainResource.ProjectManagement
            ) {
                val projectManagementContributorRegisteredEventJson = objectMapper.writeValueAsString(message.messageData)
                val contributorRegisteredEvent =
                    objectMapper.readValue(
                        projectManagementContributorRegisteredEventJson,
                        ProjectManagementContributorRegistered::class.java,
                    )

                service.createContributorAccountsForProjectManagement(
                    projectManagementId = contributorRegisteredEvent.projectManagementId,
                    contributorId = contributorRegisteredEvent.registeredContributorId,
                    requiresOwnershipAccount =
                        contributorRegisteredEvent.relevantProjectManagementBylaws
                            .find {
                                it.scope ==
                                    BylawWellknownScope.OWNERSHIP_IS_A6MANAGED.name
                            }?.definition as Boolean?
                            ?: false,
                )
            }
        }
}
