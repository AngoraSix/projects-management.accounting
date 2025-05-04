package com.angorasix.projects.management.accounting.messaging.router

import com.angorasix.commons.infrastructure.intercommunication.messaging.A6InfraMessageDto
import com.angorasix.commons.infrastructure.intercommunication.projectmanagement.ProjectManagementContributorRegistered
import com.angorasix.projects.management.accounting.messaging.handler.AccountingMessagingHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
@Configuration
class AccountingMessagingRouter(
    val handler: AccountingMessagingHandler,
) {
    @Bean
    fun createContributorAccountsForMgmt(): (A6InfraMessageDto<ProjectManagementContributorRegistered>) -> Unit =
        { handler.createContributorAccountsForMgmt(it) }
}
