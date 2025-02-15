package com.angorasix.projects.management.accounting.messaging.router

import com.angorasix.commons.infrastructure.intercommunication.dto.messaging.A6InfraMessageDto
import com.angorasix.projects.management.accounting.messaging.handler.ProjectsManagementAccountingMessagingHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
@Configuration
class ProjectsManagementAccountingMessagingRouter(
    val handler: ProjectsManagementAccountingMessagingHandler,
) {
    @Bean
    fun batchImport(): (A6InfraMessageDto) -> Unit = { handler.processBatchImport(it) }
}
