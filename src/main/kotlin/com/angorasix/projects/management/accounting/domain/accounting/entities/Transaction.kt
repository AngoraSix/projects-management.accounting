package com.angorasix.projects.management.accounting.domain.accounting.entities

import java.time.Instant

/**
 * Entity as a Value Object representing a transaction.
 */
data class Transaction(
    val transactionId: String,
    val contributorAccountId: String,
    val valueOperations: List<TransactionOperation>,
    val registeredInstant: Instant,
    val transactionSource: TransactionSource,
    // possibly other fields
)

/**
 * Value Oject representing the source of this transaction (e.g. a particular Task)
 */
data class TransactionSource(
    val sourceType: String, // e.g. A6DomainResource.Task, "transfer", "refund", ...
    val sourceId: String, // e.g. taskId, accountId
    val sourceOperation: String, // e.g. "closedTask", "revert", "executed", ...
    // possibly other fields
)

data class TransactionOperation(
    val balanceEffect: BalanceEffect,
    val valueDistribution: List<TimeBasedDistribution>,
    val fullyDefinedInstant: Instant,
) {
    fun signedCurrentAmount(): Double {
        val area = valueDistribution.sumOf { it.integrateToNow() }
        return area * balanceEffect.multiplier
    }
}

enum class BalanceEffect(
    val multiplier: Int,
) {
    DEBIT(-1),
    CREDIT(1),
}
