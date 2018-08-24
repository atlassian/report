package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.report.Point
import java.math.BigDecimal
import java.math.BigDecimal.valueOf
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal class Tick(
    private val time: Instant,
    value: Double
) : Point<Instant> {
    override val x: Instant = time
    override val y: BigDecimal = valueOf(value).setScale(2, RoundingMode.HALF_UP)

    override fun labelX(): String = LocalDateTime.ofInstant(time, ZoneId.of("UTC")).toString()
}