package com.angorasix.projects.management.accounting.presentation.handler

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure
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
    /**
     * Handler for the Get ProjectManagement Accounting Stats endpoint,
     * retrieving a Mono with the requested ProjectManagementAccountingStats.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun getProjectManagementAccountingStats(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]

        val projectManagementId = request.pathVariable("projectManagementId")

        return service
            .resolveProjectManagementTasksStats(
                projectManagementId = projectManagementId,
                requestingContributor = requestingContributor as A6Contributor?,
            ).convertToDto(requestingContributor, apiConfigs, request)
            .let {
                ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait(it)
            }
    }
}
