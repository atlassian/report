package com.atlassian.performance.tools.report

import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import java.time.Duration

/**
 * [Dispersion](https://en.wikipedia.org/wiki/Statistical_dispersion) s the extent to which a distribution is stretched
 * or squeezed.
 * Examples: variance, standard deviation, interquartile range and more.
 */
data class DispersionCriteria(
    val maxDispersionDifference: Duration,
    val dispersionMetric: UnivariateStatistic = StandardDeviation()
)