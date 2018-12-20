package com.atlassian.performance.tools.report.chart.waterfall

import java.time.Duration

internal class PhaseStackBuilder {
    private val phases = mutableMapOf<Phase, Duration>()
    private var latestEvent = Duration.ZERO

    fun push(
        precedingIdle: Phase?,
        phase: Phase,
        start: Duration,
        end: Duration
    ) : PhaseStackBuilder {
        if (precedingIdle != null) {
            if (start.isZero) {
                phases[precedingIdle] = Duration.ZERO
            } else {
                put(precedingIdle, latestEvent, start)
            }
        }
        put(phase, start, end)
        return this
    }

    fun build() = phases

    private fun put(
        phase: Phase,
        start: Duration,
        end: Duration
    ) {
        val duration = end - start
        if (duration.isNegative) throw Exception("Phase of negative duration is not allowed")
        if (duration != Duration.ZERO && start < latestEvent) throw Exception("The start of this phase is before the end of previously added phase")
        phases[phase] = duration
        latestEvent += duration
    }
}