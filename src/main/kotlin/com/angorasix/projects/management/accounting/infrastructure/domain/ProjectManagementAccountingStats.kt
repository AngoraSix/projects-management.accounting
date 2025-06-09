package com.angorasix.projects.management.accounting.infrastructure.domain

data class ProjectManagementAccountingStats(
    val projectManagementId: String,
    val project: ProjectAccountingStats,
    val contributor: ContributorAccountingStats? = null,
)

data class ProjectAccountingStats(
    val ownership: AccountStats,
    val finance: List<AccountStats> = emptyList(),
)

data class ContributorAccountingStats(
    val contributorId: String,
    val ownership: AccountStats,
    val finance: List<AccountStats> = emptyList(),
)

data class AccountStats(
    val balance: Double,
    val forecastedBalance: Map<String, Double>,
    val currency: String,
)
