package com.angorasix.projects.management.accounting.presentation.dto

import org.springframework.hateoas.RepresentationModel

data class ProjectManagementAccountingStatsDto(
    val projectManagementId: String,
    val project: ProjectAccountingStatsDto,
    val contributor: ContributorAccountingStatsDto? = null,
) : RepresentationModel<ProjectManagementAccountingStatsDto>()

data class ProjectAccountingStatsDto(
    val ownership: AccountStatsDto,
    val finance: List<AccountStatsDto> = emptyList(),
)

data class ContributorAccountingStatsDto(
    val contributorId: String,
    val ownership: AccountStatsDto,
    val finance: List<AccountStatsDto> = emptyList(),
)

data class AccountStatsDto(
    val balance: Double,
    val currency: String,
)
