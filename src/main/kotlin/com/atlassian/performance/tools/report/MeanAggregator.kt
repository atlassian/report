package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.report.api.result.InteractionStats
import org.apache.commons.math3.stat.descriptive.moment.Mean

internal class MeanAggregator {

    fun aggregateCenters(labels: List<String>, stats: InteractionStats): Long? {
        val aggregate = Mean()
        val samples = labels.mapNotNull { sampleFor(it, stats) }
        val centers = samples.map { it.center }.toDoubleArray()
        val sampleSizes = samples.map { it.sampleSize }.map { it.toDouble() }.toDoubleArray()

        return when {
            sampleSizes.all { it == 0.0 } -> null
            else -> aggregate.evaluate(centers, sampleSizes).toLong()
        }
    }

    private fun sampleFor(
        action: String,
        stats: InteractionStats
    ): Sample? {
        val center = stats.centers?.get(action)?.toMillis()?.toDouble()
        val sampleSize = stats.sampleSizes?.get(action)

        return if (center != null && sampleSize != null) {
            Sample(center, sampleSize)
        } else {
            null
        }
    }
}