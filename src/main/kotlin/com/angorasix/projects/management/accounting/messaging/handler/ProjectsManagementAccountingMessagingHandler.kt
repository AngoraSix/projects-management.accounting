package com.angorasix.projects.management.accounting.messaging.handler

import com.angorasix.commons.infrastructure.intercommunication.dto.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.dto.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.dto.messaging.A6InfraMessageDto
import com.angorasix.commons.infrastructure.intercommunication.dto.syncing.A6InfraBulkSyncingCorrespondenceDto
import com.angorasix.projects.management.accounting.application.IntegrationAccountingService
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
class ProjectsManagementAccountingMessagingHandler(
    private val service: IntegrationAccountingService,
    private val objectMapper: ObjectMapper,
) {

    fun processBatchImport(message: A6InfraMessageDto) = runBlocking {
        // TBD all this
        if (message.topic == A6InfraTopics.TASKS_INTEGRATION_SYNCING_CORRESPONDENCE.value &&
            message.targetType == A6DomainResource.IntegrationSourceSync
        ) {
            val correspondencesBulkJson = objectMapper.writeValueAsString(message.messageData)
            val correspondencesBulk = objectMapper.readValue(
                correspondencesBulkJson,
                A6InfraBulkSyncingCorrespondenceDto::class.java,
            )

            val tbd =
                correspondencesBulk.collection.map { Pair(it.integrationId, it.a6Id) }
            val (sourceSyncId, syncingEventId) = message.targetId.split(":")
//            service.tbd(
//                tbd,
//                sourceSyncId,
//                syncingEventId,
//                message.requestingContributor,
//            )
        }
    }
}
