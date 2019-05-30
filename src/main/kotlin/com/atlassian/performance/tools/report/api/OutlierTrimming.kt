package com.atlassian.performance.tools.report.api

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic

class OutlierTrimming(
    private val lowerTrim: Double,
    private val upperTrim: Double
) {

    fun measureWithoutOutliers(
        data: DescriptiveStatistics,
        metric: UnivariateStatistic
    ): Double {
        val lowerBound = Math.floor(data.n * lowerTrim).toInt()
        val upperBound = Math.ceil(data.n * upperTrim).toInt()
        val length = upperBound - lowerBound
        return metric.evaluate(data.sortedValues, lowerBound, length)
    }
}
