package com.atlassian.performance.tools.report.distribution

/**
 * Represents the [quantile function](https://en.wikipedia.org/wiki/Quantile_function).
 */
class QuantileFunction {

    fun plot(
        data: Collection<Number>
    ): List<Quantile> {
        if (data.isEmpty()) {
            return emptyList()
        }
        val distribution = RoughEmpiricalDistribution(
            binCount = 1000,
            data = data.map { it.toDouble() }.toDoubleArray()
        )
        return (0..100)
            .map { percentileIndex -> percentileIndex.toDouble() / 100 }
            .map { cumulativeProbability ->
                Quantile(
                    cumulativeProbability = cumulativeProbability,
                    value = distribution.inverseCumulativeProbability(cumulativeProbability)
                )
            }
    }
}
