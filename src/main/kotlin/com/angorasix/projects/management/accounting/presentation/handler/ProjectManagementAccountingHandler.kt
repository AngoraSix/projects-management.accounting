package com.angorasix.projects.management.accounting.presentation.handler

import com.angorasix.projects.management.accounting.application.IntegrationAccountingService
import com.angorasix.projects.management.accounting.infrastructure.config.configurationproperty.api.ApiConfigs
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * ProjectManagementIntegration Handler (Controller) containing all handler functions
 * related to ProjectManagementIntegration endpoints.
 *
 * @author rozagerardo
 */
class ProjectManagementAccountingHandler(
    private val service: IntegrationAccountingService,
    private val apiConfigs: ApiConfigs,
    private val objectMapper: ObjectMapper,
)
