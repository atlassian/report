package com.atlassian.performance.tools.report.result

import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.api.result.Stats
import java.time.Duration

data class PerformanceStats(
    override val cohort: String,
    override val sampleSizes: Map<String, Long>,
    override val locations: Map<String, Duration>,
    override val dispersions: Map<String, Duration>,
    override val errors: Map<String, Int>
) : Stats {

    companion object Adapter {
        fun adapt(stats: InteractionStats) = PerformanceStats(
            cohort = stats.cohort,
            sampleSizes = stats.sampleSizes ?: emptyMap(),
            locations = stats.centers ?: emptyMap(),
            dispersions = stats.dispersions ?: emptyMap(),
            errors = stats.errors ?: emptyMap()
        )
    }
}