package com.angorasix.projects.management.accounting.domain.accounting.entities

import java.time.Duration
import java.time.Instant

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

object TimeBasedDistributionFactory {
    // Factory method for "financial" distributions.
    fun distributionForFinancial(
        distributionType: DistributionType,
        mainValue: Double,
        startInstant: Instant,
        duration: Duration,
    ): TimeBasedDistribution =
        when (distributionType) {
            DistributionType.LINEAR_UP ->
                LinearFunctionUp(
                    mainValue = mainValue,
                    startInstant = startInstant,
                    duration = duration,
                    peakValue = mainValue,
                )

            DistributionType.LINEAR_DOWN ->
                LinearFunctionDown(
                    mainValue = mainValue,
                    startInstant = startInstant,
                    duration = duration,
                    peakValue = mainValue,
                )

            DistributionType.IMPULSE ->
                LinearFunctionDown(
                    mainValue = mainValue,
                    startInstant = startInstant,
                    duration = Duration.ZERO,
                    peakValue = mainValue,
                )
            // Handle other types accordingly...
        }

    // You can also expose additional factory methods (e.g. for ownership)
    // that compute the necessary peakValue from the provided area,
    // then call the appropriate constructor.
    fun distributionForOwnership(
        distributionType: DistributionType,
        mainValue: Double,
        startInstant: Instant,
        duration: Duration,
    ): TimeBasedDistribution =
        when (distributionType) {
            DistributionType.LINEAR_UP ->
                LinearFunctionUp(
                    mainValue = mainValue,
                    startInstant = startInstant,
                    duration = duration,
                    peakValue = resolveLinearFunctionPeak(mainValue, duration),
                )

            DistributionType.LINEAR_DOWN ->
                LinearFunctionDown(
                    mainValue = mainValue,
                    startInstant = startInstant,
                    duration = duration,
                    peakValue = resolveLinearFunctionPeak(mainValue, duration),
                )

            DistributionType.IMPULSE ->
                LinearFunctionDown(
                    mainValue = mainValue,
                    startInstant = startInstant,
                    duration = Duration.ZERO,
                    peakValue = mainValue,
                )
            // Handle other types accordingly...
        }

    /**
     * For a linear ramp, mainValue represents the Area Value.
     * The full area is (peakValue * duration) / 2.
     * To obtain the peakValue such that the area equals areaValue.
     */
    private fun resolveLinearFunctionPeak(
        areaValue: Double,
        duration: Duration,
    ) = (2 * areaValue) / duration.toMillis().toDouble()
}

// Nested implementation for a linear function that goes up.
class LinearFunctionUp internal constructor(
    override val mainValue: Double,
    override val startInstant: Instant,
    override val duration: Duration,
    private val peakValue: Double,
) : TimeBasedDistribution {
    override val functionType: DistributionType = DistributionType.LINEAR_UP

    override fun evaluateAt(t: Instant): Double {
        if (t.isBefore(startInstant)) return 0.0
        val elapsed = (t.toEpochMilli() - startInstant.toEpochMilli()).coerceAtMost(duration.toMillis()).toDouble()
        val fraction = elapsed / duration.toMillis().toDouble() // fraction in [0, 1]
        return peakValue * fraction
    }

    override fun integrateFromTo(
        from: Instant,
        to: Instant,
    ): Double {
        val startMillis = startInstant.toEpochMilli()
        val a = ((from.toEpochMilli() - startMillis).coerceAtLeast(0L)).toDouble()
        val b = ((to.toEpochMilli() - startMillis).coerceAtMost(duration.toMillis())).toDouble()
        return peakValue / (2 * duration.toMillis().toDouble()) * (b * b - a * a)
    }
}

// Nested implementation for a linear function that goes down.
class LinearFunctionDown internal constructor(
    override val mainValue: Double,
    override val startInstant: Instant,
    override val duration: Duration,
    private val peakValue: Double,
) : TimeBasedDistribution {
    override val functionType: DistributionType = DistributionType.LINEAR_DOWN

    override fun evaluateAt(t: Instant): Double {
        if (t.isBefore(startInstant)) return peakValue
        val elapsed = (t.toEpochMilli() - startInstant.toEpochMilli()).coerceAtMost(duration.toMillis()).toDouble()
        val fraction = elapsed / duration.toMillis().toDouble()
        return peakValue * (1 - fraction)
    }

    override fun integrateFromTo(
        from: Instant,
        to: Instant,
    ): Double {
        val startMillis = startInstant.toEpochMilli()
        val a = ((from.toEpochMilli() - startMillis).coerceAtLeast(0L)).toDouble()
        val b = ((to.toEpochMilli() - startMillis).coerceAtMost(duration.toMillis())).toDouble()

        fun integrate(x: Double) = peakValue * (x - (x * x) / (2 * duration.toMillis().toDouble()))
        return integrate(b) - integrate(a)
    }
}

// Nested implementation for an impulse function, giving a particular value at an instant.
class Impulse internal constructor(
    override val mainValue: Double,
    override val startInstant: Instant,
    override val duration: Duration,
    private val peakValue: Double,
) : TimeBasedDistribution {
    override val functionType: DistributionType = DistributionType.IMPULSE

    override fun evaluateAt(t: Instant): Double = if (t == startInstant) peakValue else 0.0

    override fun integrateFromTo(
        from: Instant,
        to: Instant,
    ): Double =
        if (from.isBefore(startInstant) && to.isAfter(startInstant)) {
            peakValue
        } else {
            0.0
        }
}

enum class DistributionType(
    val clazz: Class<out TimeBasedDistribution>,
) {
    LINEAR_UP(LinearFunctionUp::class.java),
    LINEAR_DOWN(LinearFunctionDown::class.java),
    IMPULSE(Impulse::class.java),
    // ...other types
}
