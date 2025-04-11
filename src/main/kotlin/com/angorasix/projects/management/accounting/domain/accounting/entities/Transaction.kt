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
    val entryType: EntryType,
    val valueDistribution: TimeBasedDistribution,
    val fullyDefinedInstant: Instant,
)

enum class EntryType {
    DEBIT,
    CREDIT,
}
