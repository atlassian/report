package com.atlassian.performance.tools.report.api

import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class OutlierTrimmingTest {

    @Test
    fun shouldMeasureWithoutOutliers() {
        val mean = 1.0
        val sd = 0.05
        val outlier1 = 0.01
        val outlier2 = 20.0
        val sample = NormalDistribution(mean, sd).sample(98)
        val normal = DescriptiveStatistics(sample)
        val withOutliers = DescriptiveStatistics(sample + outlier1 + outlier2)
        val metric = Mean()
        val trimming = OutlierTrimming(.01, .99)

        assertThat(
                trimming.measureWithoutOutliers(normal, metric)
        ).isEqualTo(
                trimming.measureWithoutOutliers(withOutliers, metric)
        )
    }
}
