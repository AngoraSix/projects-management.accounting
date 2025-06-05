package com.angorasix.projects.management.accounting.infrastructure.persistence.repository

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.querymodel.ContributorAccountView
import com.angorasix.projects.management.accounting.infrastructure.queryfilters.ListAccountingFilter
import kotlinx.coroutines.flow.Flow

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
interface AccountingInfraRepository {
    fun findUsingFilter(
        filter: ListAccountingFilter,
        requestingContributor: A6Contributor?,
    ): Flow<ContributorAccountView>

    suspend fun findSingleUsingFilter(
        filter: ListAccountingFilter,
        requestingContributor: A6Contributor?,
    ): ContributorAccountView?
}
