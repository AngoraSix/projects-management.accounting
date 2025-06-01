package com.angorasix.projects.management.accounting.infrastructure.queryfilters

import com.angorasix.projects.management.accounting.domain.accounting.aggregates.ContributorAccount

/**
 * <p>
 *     Classes containing different Request Query Filters.
 * </p>
 *
 * @author rozagerardo
 */
data class ListAccountingFilter(
    val projectManagementId: Set<String>? = null,
    val contributorId: Set<String>? = null,
    val adminId: Set<String>? = null,
    val ids: Collection<String>? = null, // task ids
    val accountStatus: Set<ContributorAccount.ContributorAccountStatusValues>? = null, // task status
)
