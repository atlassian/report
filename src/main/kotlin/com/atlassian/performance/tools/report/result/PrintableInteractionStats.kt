package com.atlassian.performance.tools.report.result

import com.atlassian.performance.tools.report.MeanAggregator
import com.atlassian.performance.tools.report.api.result.InteractionStats

internal class PrintableInteractionStats(
    stats: InteractionStats,
    actions: List<String>
) {
    val cohort: String = stats.cohort
    val sampleSizes: Map<String, String> =
        stats.sampleSizes?.mapValues { (_, sampleSize) -> sampleSize.toString() } ?: emptyMap()
    val centers: Map<String, String> =
        stats.centers?.mapValues { (_, center) -> center.toMillis().toString() } ?: emptyMap()
    val dispersions: Map<String, String> =
        stats.dispersions?.mapValues { (_, dispersion) -> dispersion.toMillis().toString() } ?: emptyMap()
    val errors: Map<String, String> =
        stats.errors?.mapValues { (_, errorCount) -> errorCount.toString() } ?: emptyMap()

    val mean: String = MeanAggregator().aggregateCenters(actions, stats)?.toString() ?: ""
    val requestCount: String = actions.map { stats.sampleSizes?.get(it) ?: 0 }.sum().toString()
    val errorCount: String = actions.map { stats.errors?.get(it) ?: 0 }.sum().toString()

}