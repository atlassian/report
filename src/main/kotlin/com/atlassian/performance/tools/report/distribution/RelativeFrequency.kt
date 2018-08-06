package com.atlassian.performance.tools.report.distribution

import com.atlassian.performance.tools.report.Point
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents relative [frequency](https://en.wikipedia.org/wiki/Frequency_(statistics)).
 */
data class RelativeFrequency(
    val valueRange: ClosedRange<Double>,
    val frequency: Double
) : Point<Int> {

    override val x: Int = listOf(valueRange.start, valueRange.endInclusive).average().toInt()
    override val y: BigDecimal = (frequency * 100)
        .toBigDecimal()
        .setScale(2, RoundingMode.HALF_UP)

    override fun labelX(): String = "${valueRange.start.toInt()}-${valueRange.endInclusive.toInt()}"
}