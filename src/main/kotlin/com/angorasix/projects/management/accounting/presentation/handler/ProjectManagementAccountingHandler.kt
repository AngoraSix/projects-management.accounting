package com.angorasix.projects.management.accounting.presentation.handler

import com.angorasix.projects.management.accounting.application.AccountingService
import com.angorasix.projects.management.accounting.infrastructure.config.configurationproperty.api.ApiConfigs
import org.springframework.hateoas.MediaTypes
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait

/**
 * ProjectManagementIntegration Handler (Controller) containing all handler functions
 * related to ProjectManagementIntegration endpoints.
 *
 * @author rozagerardo
 */
class ProjectManagementAccountingHandler(
    private val service: AccountingService,
    private val apiConfigs: ApiConfigs,
) {
    suspend fun tbd(request: ServerRequest): ServerResponse {
        println("TBD: $service $apiConfigs $request")
        return ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait("TBD")
    }
}
