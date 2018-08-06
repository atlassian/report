package com.atlassian.performance.tools.report.distribution

/**
 * Represents the [histogram](https://en.wikipedia.org/wiki/Histogram).
 */
class Histogram {

    fun plot(
        data: Collection<Number>
    ): List<RelativeFrequency> {
        if (data.isEmpty()) {
            return emptyList()
        }
        val dataSize = data.size
        val distribution = RoughEmpiricalDistribution(
            binCount = 50,
            data = data.map { it.toDouble() }.toDoubleArray()
        )
        val bounds = arrayOf(distribution.sampleStats.min) + distribution.upperBounds.toTypedArray()
        return distribution.binStats.mapIndexed { index, stats ->
            RelativeFrequency(
                valueRange = bounds[index]..bounds[index + 1],
                frequency = stats.n.toDouble() / dataSize
            )
        }
    }
}
