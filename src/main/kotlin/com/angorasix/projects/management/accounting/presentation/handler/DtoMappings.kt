package com.angorasix.projects.management.accounting.presentation.handler

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.projects.management.accounting.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.accounting.infrastructure.domain.AccountStats
import com.angorasix.projects.management.accounting.infrastructure.domain.ContributorAccountingStats
import com.angorasix.projects.management.accounting.infrastructure.domain.ProjectAccountingStats
import com.angorasix.projects.management.accounting.infrastructure.domain.ProjectManagementAccountingStats
import com.angorasix.projects.management.accounting.presentation.dto.AccountStatsDto
import com.angorasix.projects.management.accounting.presentation.dto.ContributorAccountingStatsDto
import com.angorasix.projects.management.accounting.presentation.dto.ProjectAccountingStatsDto
import com.angorasix.projects.management.accounting.presentation.dto.ProjectManagementAccountingStatsDto
import org.springframework.web.reactive.function.server.ServerRequest

fun ProjectManagementAccountingStats.convertToDto(): ProjectManagementAccountingStatsDto =
    ProjectManagementAccountingStatsDto(
        projectManagementId,
        project.convertToDto(),
        contributor?.convertToDto(),
    )

fun ProjectManagementAccountingStats.convertToDto(
    requestingContributor: A6Contributor?,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): ProjectManagementAccountingStatsDto = convertToDto().resolveHypermedia(this, apiConfigs, request)

fun ContributorAccountingStats.convertToDto(): ContributorAccountingStatsDto =
    ContributorAccountingStatsDto(
        contributorId = contributorId,
        ownership = ownership.convertToDto(),
        finance = finance.map { it.convertToDto() },
    )

fun ProjectAccountingStats.convertToDto(): ProjectAccountingStatsDto =
    ProjectAccountingStatsDto(
        ownership = ownership.convertToDto(),
        finance = finance.map { it.convertToDto() },
    )

fun AccountStats.convertToDto(): AccountStatsDto =
    AccountStatsDto(
        balance,
        currency,
    )
