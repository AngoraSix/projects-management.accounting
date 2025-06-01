package com.angorasix.projects.management.accounting.presentation.handler

import com.angorasix.commons.reactive.presentation.mappings.addSelfLink
import com.angorasix.projects.management.accounting.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.accounting.infrastructure.domain.ProjectManagementAccountingStats
import com.angorasix.projects.management.accounting.presentation.dto.ProjectManagementAccountingStatsDto
import org.springframework.web.reactive.function.server.ServerRequest

fun ProjectManagementAccountingStatsDto.resolveHypermedia(
    projectManagementAccountingStats: ProjectManagementAccountingStats,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): ProjectManagementAccountingStatsDto {
    val getSingleRoute = apiConfigs.routes.getProjectManagementAccountingStats
    // self
    addSelfLink(getSingleRoute, request, listOf(projectManagementAccountingStats.projectManagementId))
    return this
}
