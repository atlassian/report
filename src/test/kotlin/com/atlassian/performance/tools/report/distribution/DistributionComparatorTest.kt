package com.atlassian.performance.tools.report.distribution

import com.atlassian.performance.tools.report.api.distribution.DistributionComparator
import com.atlassian.performance.tools.report.api.result.FakeResults
import com.atlassian.performance.tools.report.chart.Chart
import com.atlassian.performance.tools.report.chart.ChartLine
import com.atlassian.performance.tools.workspace.api.git.GitRepo
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
import org.apache.commons.math3.random.MersenneTwister
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.*
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Files

class DistributionComparatorTest {

    /**
     * https://en.wikipedia.org/wiki/Robust_statistics
     */
    @Test
    fun shouldBeRobust() {
        // given
        val random = MersenneTwister(123)
        val baseline = NormalDistribution(random, 400.0, 30.0, DEFAULT_INVERSE_ABSOLUTE_ACCURACY).sample(500)
        val outlier = 1_000_000_000_000.0
        val experiment = baseline + outlier

        // when
        val comparison = DistributionComparator.Builder(baseline, experiment)
            .tolerance(0.0)
            .build().compare()

        // then
        plotQuantiles(baseline, experiment)
        assertSoftly {
            it.assertThat(comparison.experimentAbsoluteChange).`as`("absolute change").isEqualTo(2.096466300542943E-4)
            it.assertThat(comparison.experimentRelativeChange).`as`("relative change").isEqualTo(5.05322869548408E-7)
            it.assertThat(comparison.isExperimentImproved).`as`("improved").isFalse
            it.assertThat(comparison.isExperimentRegressed).`as`("regressed").isFalse
        }
    }

    @Test
    fun shouldDescribeConstantSlowdown() {
        // given
        val random = MersenneTwister(123)
        val fastMode = NormalDistribution(random, 400.0, 30.0, DEFAULT_INVERSE_ABSOLUTE_ACCURACY)
        val slowMode = NormalDistribution(random, 6000.0, 150.0, DEFAULT_INVERSE_ABSOLUTE_ACCURACY)
        val baseline = fastMode.sample(500) + slowMode.sample(90)
        val slowdown = 800.0
        val experiment = baseline.map { it + slowdown }.toDoubleArray()

        // when
        val comparison = DistributionComparator.Builder(baseline, experiment).build().compare()

        // then
        plotQuantiles(baseline, experiment)
        assertSoftly {
            it.assertThat(comparison.experimentAbsoluteChange).`as`("absolute change").isEqualTo(800.0)
            it.assertThat(comparison.experimentRelativeChange).`as`("relative change").isEqualTo(1.9951194021713592)
            it.assertThat(comparison.isExperimentImproved).`as`("regressed").isFalse
            it.assertThat(comparison.isExperimentRegressed).`as`("regressed").isTrue
        }
    }

    @Test
    fun shouldDescribePartialSlowdown() {
        // given
        val random = MersenneTwister(123)
        val fastMode = NormalDistribution(random, 400.0, 30.0, DEFAULT_INVERSE_ABSOLUTE_ACCURACY)
        val slowMode = NormalDistribution(random, 6000.0, 150.0, DEFAULT_INVERSE_ABSOLUTE_ACCURACY)
        val baseline = fastMode.sample(500) + slowMode.sample(90)
        val verticalShift = 800.0
        val partBoundary = 0.40 * baseline.size
        val experiment = baseline
            .mapIndexed { index, latency ->
                if (index < partBoundary) {
                    latency + verticalShift
                } else {
                    latency
                }
            }
            .toDoubleArray()

        // when
        val comparison = DistributionComparator.Builder(baseline, experiment).build().compare()

        // then
        plotQuantiles(baseline, experiment)
        assertSoftly {
            it.assertThat(comparison.experimentAbsoluteChange).`as`("absolute change").isEqualTo(66.57177057231809)
            it.assertThat(comparison.experimentRelativeChange).`as`("relative change").isEqualTo(0.16145163153504116)
            it.assertThat(comparison.isExperimentImproved).`as`("improved").isFalse
            it.assertThat(comparison.isExperimentRegressed).`as`("regressed").isTrue
        }
    }

    /**
     * In a 51% slightly faster vs 49% much slower case, it should be a regression
     */
    @Test
    @Ignore
    fun shouldCareAboutHeightOfTheDifferences() {
        // given
        val random = MersenneTwister(123)
        val fastMode = NormalDistribution(random, 400.0, 30.0, DEFAULT_INVERSE_ABSOLUTE_ACCURACY)
        val baseline = fastMode.sample(500)
        val slowdown = 800.0
        val speedup = -200.0
        val partBoundary = 0.51 * baseline.size
        val experiment = baseline
            .sorted()
            .mapIndexed { index, latency ->
                if (index < partBoundary) {
                    latency + speedup
                } else {
                    latency + slowdown
                }
            }
            .toDoubleArray()

        // when
        val comparison = DistributionComparator.Builder(baseline, experiment).build().compare()

        // then
        plotQuantiles(baseline, experiment)
        assertSoftly {
            it.assertThat(comparison.experimentAbsoluteChange).`as`("absolute change").isPositive
            it.assertThat(comparison.experimentRelativeChange).`as`("relative change").isPositive
            it.assertThat(comparison.isExperimentImproved).`as`("improvement").isFalse
            it.assertThat(comparison.isExperimentRegressed).`as`("regressed").isTrue
        }
    }

    @Test
    fun shouldDetectImprovementWhenEveryPercentileBetter() {
        // given
        val baseline =
            this.javaClass.getResource("/real-results/view issue 9.17.0 vs 10.0.0/baseline.csv").readText().lines()
                .map { it.toDouble() }.toDoubleArray()
        val experiment =
            this.javaClass.getResource("/real-results/view issue 9.17.0 vs 10.0.0/experiment.csv").readText().lines()
                .map { it.toDouble() }.toDoubleArray()
        // when
        val comparison = DistributionComparator.Builder(baseline, experiment).build().compare()
        // then
        plotQuantiles(baseline, experiment)
        assertThat(comparison.isExperimentImproved).isTrue()
        assertThat(comparison.isExperimentRegressed).isFalse()
        assertThat(comparison.experimentRelativeChange).isEqualTo(-0.03941908713692943)
    }


    private fun plotQuantiles(
        baseline: DoubleArray,
        experiment: DoubleArray
    ) {
        val chart = Chart(
            listOf(
                chartLine(baseline, "baseline"),
                chartLine(experiment, "experiment")
            )
        )
        val htmlFile = Files.createTempFile("distribution-quantiles", ".html")
            .also { println("Distribution comparison at $it") }
        DistributionComparison(GitRepo.findFromCurrentDirectory()).render(chart, htmlFile)
    }

    private fun chartLine(data: DoubleArray, label: String) = ChartLine(
        data = QuantileFunction().plot(data.toList()),
        label = label,
        type = "line",
        yAxisId = "latency-axis"
    )

    @Test
    fun shouldSeeNoShiftAcrossTheSameResult() {
        // given
        val result = FakeResults.fastResult
            .actionMetrics.map { it.duration.toMillis() }
            .map { it.toDouble() }.toDoubleArray()

        // when
        val comparison = DistributionComparator.Builder(result, result).build().compare()

        // then
        assertThat(comparison.experimentAbsoluteChange).isEqualTo(0.0)
        assertThat(comparison.experimentRelativeChange).isEqualTo(0.0)
        assertThat(comparison.hasImpact()).isFalse()
    }
}
