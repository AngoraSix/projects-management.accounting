package com.angorasix.projects.management.accounting.infrastructure.service

import com.angorasix.projects.management.accounting.application.AccountingService
import com.angorasix.projects.management.accounting.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.projections.ContributorAccountProjection
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.repository.ContributorAccountViewRepository
import com.angorasix.projects.management.accounting.messaging.handler.AccountingMessagingHandler
import com.angorasix.projects.management.accounting.presentation.handler.ProjectManagementAccountingHandler
import com.angorasix.projects.management.accounting.presentation.router.ProjectManagementAccountingRouter
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ServiceConfiguration {
    @Bean
    fun accountingService(
        repository: ContributorAccountViewRepository,
        commandGateway: CommandGateway,
    ): AccountingService = AccountingService(repository, commandGateway)

    @Bean
    fun projectManagementAccountingHandler(
        service: AccountingService,
        apiConfigs: ApiConfigs,
    ) = ProjectManagementAccountingHandler(service, apiConfigs)

    @Bean
    fun projectsManagementAccountingMessagingHandler(service: AccountingService) = AccountingMessagingHandler(service)

    @Bean
    fun projectManagementAccountingRouter(
        handler: ProjectManagementAccountingHandler,
        apiConfigs: ApiConfigs,
    ) = ProjectManagementAccountingRouter(handler, apiConfigs).projectRouterFunction()

    @Bean
    fun contributorAccountProjection(repository: ContributorAccountViewRepository): ContributorAccountProjection =
        ContributorAccountProjection(repository)
}
