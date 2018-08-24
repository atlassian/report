package com.atlassian.performance.tools.report.distribution

import com.atlassian.performance.tools.report.Point
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents a [quantile](https://en.wikipedia.org/wiki/Quantile).
 */
internal data class Quantile(
    val cumulativeProbability: Double,
    val value: Double
) : Point<Double> {
    override val x: Double = cumulativeProbability
    override val y: BigDecimal = value
        .toBigDecimal()
        .setScale(0, RoundingMode.HALF_UP)

    override fun labelX(): String = (cumulativeProbability * 100)
        .toBigDecimal()
        .setScale(0, RoundingMode.HALF_UP)
        .toString()
}