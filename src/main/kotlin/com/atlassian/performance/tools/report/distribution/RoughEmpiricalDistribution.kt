package com.atlassian.performance.tools.report.distribution

import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.random.EmpiricalDistribution
import org.apache.commons.math3.stat.descriptive.SummaryStatistics

/**
 * Works around [MATH-1462](https://issues.apache.org/jira/browse/MATH-1462).
 */
class RoughEmpiricalDistribution(
    binCount: Int,
    data: DoubleArray
) : EmpiricalDistribution(binCount) {

    init {
        super.load(data)
    }

    override fun getKernel(
        bStats: SummaryStatistics
    ): RealDistribution {
        return ConstantRealDistribution(bStats.mean)
    }
}