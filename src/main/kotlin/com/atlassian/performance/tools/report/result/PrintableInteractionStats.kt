package com.atlassian.performance.tools.report.result

import com.atlassian.performance.tools.report.MeanAggregator
import com.atlassian.performance.tools.report.api.result.Stats

internal class PrintableInteractionStats(
    stats: Stats,
    actions: List<String>
) {
    val cohort: String = stats.cohort
    val sampleSizes: Map<String, String> =
        stats.sampleSizes.mapValues { (_, sampleSize) -> sampleSize.toString() }
    val centers: Map<String, String> =
        stats.locations.mapValues { (_, center) -> center.toMillis().toString() }
    val dispersions: Map<String, String> =
        stats.dispersions.mapValues { (_, dispersion) -> dispersion.toMillis().toString() }
    val errors: Map<String, String> =
        stats.errors.mapValues { (_, errorCount) -> errorCount.toString() }

    val mean: String = MeanAggregator().aggregateCenters(actions, stats)?.toString() ?: ""
    val requestCount: String = actions.map { stats.sampleSizes[it] ?: 0 }.sum().toString()
    val errorCount: String = actions.map { stats.errors[it] ?: 0 }.sum().toString()

}