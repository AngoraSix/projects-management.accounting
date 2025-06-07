package com.angorasix.projects.management.accounting.domain.accounting.entities

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Duration
import java.time.Instant

/**
 * Represents a time-based distribution function.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "distributionType",
)
@JsonSubTypes(
    JsonSubTypes.Type(LinearFunctionUp::class, name = "LINEAR_UP"),
    JsonSubTypes.Type(LinearFunctionDown::class, name = "LINEAR_DOWN"),
    JsonSubTypes.Type(Impulse::class, name = "IMPULSE"),
    JsonSubTypes.Type(Step::class, name = "STEP"),
)
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
                Impulse(
                    mainValue = mainValue,
                    startInstant = startInstant,
                    duration = Duration.ZERO,
                    peakValue = mainValue,
                )

            DistributionType.STEP ->
                Step(
                    mainValue = mainValue,
                    startInstant = startInstant,
                    duration = duration,
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
                Impulse(
                    mainValue = mainValue,
                    startInstant = startInstant,
                    duration = Duration.ZERO,
                    peakValue = mainValue,
                )

            DistributionType.STEP ->
                Step(
                    mainValue = mainValue,
                    startInstant = startInstant,
                    duration = Duration.ZERO,
                    peakValue = resolveStepFunctionPeak(mainValue, duration),
                )
            // Handle other types accordingly...
        }

    /**
     * For a linear ramp, mainValue represents the Area Value.
     * The full area is (peakValue * duration) / 2.
     * So, peakValue = 2 × areaValue ⁄ duration
     */
    private fun resolveLinearFunctionPeak(
        areaValue: Double,
        duration: Duration,
    ): Double = (2 * areaValue) / duration.toMillis().toDouble()

    /**
     * For a step, mainValue represents the Area Value.
     * The full area is peakValue * duration.
     * So, peakValue = areaValue ⁄ duration
     */
    private fun resolveStepFunctionPeak(
        areaValue: Double,
        duration: Duration,
    ): Double = areaValue / duration.toMillis().toDouble()
}

// Nested implementation for a linear function that goes up.
data class LinearFunctionUp internal constructor(
    override val mainValue: Double,
    override val startInstant: Instant,
    override val duration: Duration,
    val peakValue: Double,
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
        val rawA = (from.toEpochMilli() - startMillis).toDouble()
        val rawB = (to.toEpochMilli() - startMillis).toDouble()
        val durationMillis = duration.toMillis().toDouble()

        val a = rawA.coerceIn(0.0, durationMillis)
        val b = rawB.coerceIn(0.0, durationMillis)
        if (b <= a) return 0.0

        val area = peakValue / (2 * durationMillis) * (b * b - a * a)
        return area
    }
}

// Nested implementation for a linear function that goes down.
data class LinearFunctionDown internal constructor(
    override val mainValue: Double,
    override val startInstant: Instant,
    override val duration: Duration,
    val peakValue: Double,
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
        val rawA = (from.toEpochMilli() - startMillis).toDouble()
        val rawB = (to.toEpochMilli() - startMillis).toDouble()
        val durationMillis = duration.toMillis().toDouble()

        // clamp both ends into [0, durationMillis]:
        val a = rawA.coerceIn(0.0, durationMillis)
        val b = rawB.coerceIn(0.0, durationMillis)

        // if the “to”‐end does not exceed the “from”‐end, area is zero:
        if (b <= a) {
            return 0.0
        }

        // ∫ₐᵇ peak * (1 − t/duration) dt = peak * [ (b − b²/(2·duration)) − (a − a²/(2·duration)) ]
        fun integrate(x: Double): Double = peakValue * (x - (x * x) / (2 * durationMillis))

        return integrate(b) - integrate(a)
    }
}

// Nested implementation for an impulse function, giving a particular value at an instant.
data class Impulse internal constructor(
    override val mainValue: Double,
    override val startInstant: Instant,
    override val duration: Duration,
    val peakValue: Double,
) : TimeBasedDistribution {
    override val functionType: DistributionType = DistributionType.IMPULSE

    override fun evaluateAt(t: Instant): Double = if (t == startInstant) peakValue else 0.0

    override fun integrateFromTo(
        from: Instant,
        to: Instant,
    ): Double =
        if (from.minusMillis(1).isBefore(startInstant) && to.isAfter(startInstant)) {
            peakValue
        } else {
            0.0
        }
}

// Nested implementation for an step function, constant for the period for the peakValue.
data class Step internal constructor(
    override val mainValue: Double,
    override val startInstant: Instant,
    override val duration: Duration,
    val peakValue: Double,
) : TimeBasedDistribution {
    override val functionType: DistributionType = DistributionType.STEP

    override fun evaluateAt(t: Instant): Double =
        if (t.isBefore(startInstant).or(t.isAfter(startInstant.plus(duration)))) 0.0 else peakValue

    override fun integrateFromTo(
        from: Instant,
        to: Instant,
    ): Double {
        val startMillis = startInstant.toEpochMilli()
        val rawA = (from.toEpochMilli() - startMillis).toDouble()
        val rawB = (to.toEpochMilli() - startMillis).toDouble()
        val durationMillis = duration.toMillis().toDouble()

        val a = rawA.coerceIn(0.0, durationMillis)
        val b = rawB.coerceIn(0.0, durationMillis)
        if (b <= a) return 0.0

        val area = (b - a) * peakValue
        return area
    }
}

enum class DistributionType(
    val clazz: Class<out TimeBasedDistribution>,
) {
    LINEAR_UP(LinearFunctionUp::class.java),
    LINEAR_DOWN(LinearFunctionDown::class.java),
    IMPULSE(Impulse::class.java),
    STEP(Step::class.java),
    // ...other types
}
