package com.angorasix.projects.management.accounting.infrastructure.persistence.repository

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.querymodel.ContributorAccountView
import com.angorasix.projects.management.accounting.infrastructure.queryfilters.ListAccountingFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
class AccountingInfraRepositoryImpl(
    private val mongoOps: ReactiveMongoOperations,
) : AccountingInfraRepository {
    override fun findUsingFilter(
        filter: ListAccountingFilter,
        requestingContributor: A6Contributor?,
    ): Flow<ContributorAccountView> = mongoOps.find(filter.toQuery(requestingContributor), ContributorAccountView::class.java).asFlow()

    override suspend fun findSingleUsingFilter(
        filter: ListAccountingFilter,
        requestingContributor: A6Contributor?,
    ): ContributorAccountView? = mongoOps.find(filter.toQuery(requestingContributor), ContributorAccountView::class.java).awaitFirstOrNull()

    private fun ListAccountingFilter.toQuery(requestingContributor: A6Contributor?): Query {
        val query = Query()

        projectManagementId?.let { query.addCriteria(where("projectManagementId").`in`(it as Collection<Any>)) }
        accountStatus?.let { query.addCriteria(where("status.status").`in`(it as Collection<Any>)) }
        ids?.let { query.addCriteria(where("id").`in`(it as Collection<Any>)) }
        currency?.let { query.addCriteria(where("currency").`in`(it as Collection<Any>)) }
        accountType?.let { query.addCriteria(where("accountType").`in`(it as Collection<Any>)) }

        contributorId?.let {
            if (requestingContributor != null) {
                val requestingContributorId = requestingContributor.contributorId
                // if we're requesting a contributor, it should be either for the requesing contributor (myself)
                // or for an account administered by me
                val orCriteria =
                    mutableListOf(
                        where("contributorId").`in`(it + requestingContributorId),
                        where("contributorId").`in`(it).andOperator(where("admins.contributorId").`in`(requestingContributorId)),
                    )
                query.addCriteria(Criteria().orOperator(orCriteria))
            } else {
                query.addCriteria(where("contributorId").`in`(it))
            }
        }

        if (adminId != null && requestingContributor != null) {
            query.addCriteria(where("admins.contributorId").`in`(adminId + requestingContributor.contributorId))
        }

        return query
    }
}
