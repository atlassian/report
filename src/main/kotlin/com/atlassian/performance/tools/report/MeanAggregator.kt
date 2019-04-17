package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.report.api.result.Stats
import org.apache.commons.math3.stat.descriptive.moment.Mean

internal class MeanAggregator {

    fun aggregateCenters(labels: List<String>, stats: Stats): Long? {
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
        stats: Stats
    ): Sample? {
        val center = stats.locations[action]?.toMillis()?.toDouble()
        val sampleSize = stats.sampleSizes[action]

        return if (center != null && sampleSize != null) {
            Sample(center, sampleSize)
        } else {
            null
        }
    }
}