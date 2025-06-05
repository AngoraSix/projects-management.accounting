package com.angorasix.projects.management.accounting.infrastructure.eventsourcing.querymodel

import com.angorasix.projects.management.accounting.domain.accounting.aggregates.ContributorAccount
import com.angorasix.projects.management.accounting.domain.accounting.entities.TransactionOperation
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
//    val lastUpdatedBalance: Double, // Balance up to the last updated instant (checkpoint)
//    val unprocessedTransactionOperations: List<TransactionOperation>,
//    val processedTransactionOperations: List<TransactionOperation>,
    val lastUpdatedInstant: Instant,
    val transactionOperations: List<TransactionOperation>,
    val status: ContributorAccountStatusView,
) {
    fun copyUpdateToInstant(checkpointInstant: Instant): ContributorAccountView =
        this.copy(
            lastUpdatedInstant = checkpointInstant,
            // lastUpdatedBalance = UPDATE BALANCE UP TO THIS POINT,
            // unprocessedTransactionOperations = UPDATE UNPROCESSED TRANSACTION OPERATIONS UP TO THIS POINT…
        )

    /**
     * Retorna el balance actual, que es la suma de:
     *   • lastUpdatedBalance  (lo que ya estaba guardado en el checkpoint)
     *   • más la suma de signedAmount() de cada TransactionOperation pendiente
     */
    fun calculateCurrentBalance(): Double {
//        val pendingTotal = transactionOperations.sumOf { it.signedCurrentAmount() }
//        return lastUpdatedBalance + pendingTotal
        return transactionOperations.sumOf { it.signedCurrentAmount() }
    }
}

data class ContributorAccountStatusView(
    val status: ContributorAccount.ContributorAccountStatusValues,
    val activationDate: Instant? = null, // the account activationDate
)
