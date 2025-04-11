package com.angorasix.projects.management.accounting.infrastructure.eventsourcing.querymodel

import com.angorasix.projects.management.accounting.domain.accounting.ContributorAccountStatusValues
import com.angorasix.projects.management.accounting.domain.accounting.TransactionOperation
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "contributorAccountView")
data class ContributorAccountView(
    @Id val accountId: String,
    val projectManagementId: String,
    val contributorId: String,
    val currency: String,
    val accountType: String,
    val lastUpdatedBalance: Double, // Balance up to the last updated instant (checkpoint)
    val lastUpdatedInstant: Instant,
    val unprocessedTransactionOperations: MutableList<TransactionOperation>,
    val status: ContributorAccountStatusView,
) {
    fun copyUpdateToInstant(checkpointInstant: Instant): ContributorAccountView =
        this.copy(
            lastUpdatedInstant = checkpointInstant,
            // lastUpdatedBalance = UPDATE BALANCE UP TO THIS POINT,
            // unprocessedTransactionOperations = UPDATE UNPROCESSED TRANSACTION OPERATIONS UP TO THIS POINT…
        )
}

data class ContributorAccountStatusView(
    val status: ContributorAccountStatusValues,
    val activationDate: Instant? = null, // the account activationDate
)
