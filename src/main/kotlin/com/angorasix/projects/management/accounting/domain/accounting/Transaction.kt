package com.angorasix.projects.management.accounting.domain.accounting

import java.time.Duration
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

/**
 * Represents a time-based distribution function.
 */
interface TimeBasedDistribution {
    val functionType: DistributionType // LINEAR, STEP, etc.
    val mainValue: Double // in general, the value of the estimation output (caps)
    val duration: Duration
    val startInstant: Instant? // Might start since the point in which the account is activated, not defined beforehand

    fun evaluateAt(t: Instant): Double

    fun evaluateNow(): Double = evaluateAt(Instant.now())

    fun integrateFromTo(
        from: Instant,
        to: Instant,
    ): Double

    /**
     * Integrate the function from the start to the given instant.
     */
    fun integrateTo(to: Instant): Double =
        startInstant?.let {
            integrateFromTo(
                it,
                to,
            )
        } ?: 0.0

    fun integrateToNow(): Double = integrateTo(Instant.now())
}

class LinearFunctionUp(
    override val startInstant: Instant,
    override val duration: Duration,
    peakValue: Double,
) : TimeBasedDistribution {
    override val functionType: DistributionType = DistributionType.LINEAR_UP
    override val mainValue: Double = peakValue

    override fun evaluateAt(t: Instant): Double {
        if (t.isBefore(startInstant)) return 0.0
        val elapsed = (t.toEpochMilli() - startInstant.toEpochMilli()).coerceAtMost(duration.toMillis()).toDouble()
        val fraction = elapsed / duration.toMillis().toDouble() // fraction in [0, 1]
        return mainValue * fraction
    }

    override fun integrateFromTo(
        from: Instant,
        to: Instant,
    ): Double {
        // Closed-form integration for linear ramp from 0:
        val startMillis = startInstant.toEpochMilli()
        val a = ((from.toEpochMilli() - startMillis).coerceAtLeast(0L)).toDouble()
        val b = ((to.toEpochMilli() - startMillis).coerceAtMost(duration.toMillis())).toDouble()
        // f(t) = (mainValue/duration) * t, so ∫ from a to b f(t) dt = mainValue/(2*duration) * (b² - a²)
        return mainValue / (2 * duration.toMillis().toDouble()) * (b * b - a * a)
    }
}

class LinearFunctionDown(
    override val startInstant: Instant,
    override val duration: Duration,
    peakValue: Double,
) : TimeBasedDistribution {
    override val functionType: DistributionType = DistributionType.LINEAR_DOWN
    override val mainValue: Double = peakValue

    override fun evaluateAt(t: Instant): Double {
        // Ramp down: from mainValue at startInstant to 0 at startInstant + duration.
        if (t.isBefore(startInstant)) return mainValue
        val elapsed = (t.toEpochMilli() - startInstant.toEpochMilli()).coerceAtMost(duration.toMillis()).toDouble()
        val fraction = elapsed / duration.toMillis().toDouble() // fraction in [0, 1]
        return mainValue * (1 - fraction)
    }

    override fun integrateFromTo(
        from: Instant,
        to: Instant,
    ): Double {
        // For ramp-down: f(t) = mainValue*(1 - t/duration). Its integration from a to b:
        val startMillis = startInstant.toEpochMilli()
        val a = ((from.toEpochMilli() - startMillis).coerceAtLeast(0L)).toDouble()
        val b = ((to.toEpochMilli() - startMillis).coerceAtMost(duration.toMillis())).toDouble()

        // ∫ f(t) dt = mainValue * ( (t) - (t^2)/(2*duration) )
        fun integrate(x: Double) = mainValue * (x - (x * x) / (2 * duration.toMillis().toDouble()))
        return integrate(b) - integrate(a)
    }
}

enum class DistributionType(
    val clazz: Class<out TimeBasedDistribution>,
) {
    LINEAR_UP(LinearFunctionUp::class.java),
    LINEAR_DOWN(LinearFunctionDown::class.java),
    // ...other types
}
