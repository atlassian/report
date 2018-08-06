package com.atlassian.performance.tools.report

import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import org.apache.commons.math3.stat.descriptive.moment.Mean

/**
 * [Center](https://en.wikipedia.org/wiki/Central_tendency) is a central or a typical value for a distribution.
 * Examples: mean, median, mode and more.
 */
data class CenterCriteria(
    val toleranceRatio: Float,
    val centralTendencyMetric: UnivariateStatistic = Mean()
)