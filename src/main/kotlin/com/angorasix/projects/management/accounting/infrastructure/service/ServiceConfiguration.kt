package com.angorasix.projects.management.accounting.infrastructure.service

import com.angorasix.projects.management.accounting.application.AccountingService
import com.angorasix.projects.management.accounting.infrastructure.config.configurationproperty.amqp.AmqpConfigurations
import com.angorasix.projects.management.accounting.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.accounting.messaging.handler.ProjectsManagementAccountingMessagingHandler
import com.angorasix.projects.management.accounting.presentation.handler.ProjectManagementAccountingHandler
import com.angorasix.projects.management.accounting.presentation.router.ProjectManagementAccountingRouter
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ServiceConfiguration {
    @Bean
    fun accountingService(
        streamBridge: StreamBridge,
        amqpConfigs: AmqpConfigurations,
    ): AccountingService = AccountingService(streamBridge, amqpConfigs)

    @Bean
    fun projectManagementAccountingHandler(
        service: AccountingService,
        apiConfigs: ApiConfigs,
        objectMapper: ObjectMapper,
    ) = ProjectManagementAccountingHandler(service, apiConfigs, objectMapper)

    @Bean
    fun projectsManagementAccountingMessagingHandler(
        service: AccountingService,
        objectMapper: ObjectMapper,
    ) = ProjectsManagementAccountingMessagingHandler(service, objectMapper)

    @Bean
    fun projectManagementAccountingRouter(
        handler: ProjectManagementAccountingHandler,
        apiConfigs: ApiConfigs,
    ) = ProjectManagementAccountingRouter(handler, apiConfigs).projectRouterFunction()
}
