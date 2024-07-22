package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.result.FakeResults
import com.atlassian.performance.tools.report.chart.Chart
import com.atlassian.performance.tools.report.chart.ChartLine
import com.atlassian.performance.tools.report.distribution.DistributionComparison
import com.atlassian.performance.tools.report.distribution.QuantileFunction
import com.atlassian.performance.tools.workspace.api.git.GitRepo
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
import org.apache.commons.math3.random.MersenneTwister
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.assertj.core.data.Offset
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Files

class ShiftedDistributionRegressionTestTest {

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
        val test = ShiftedDistributionRegressionTest(baseline, experiment)

        // when
        val absoluteShift = -test.locationShift
        val relativeShift = -test.percentageShift
        val regressed = test.isExperimentRegressed(0.00)
        val sameDistro = test.equalDistributionsAfterShift

        // then
        plotQuantiles(baseline, experiment)
        assertSoftly {
            it.assertThat(absoluteShift).`as`("absolute shift").isEqualTo(2.096466300542943E-4)
            it.assertThat(relativeShift).`as`("relative shift").isEqualTo(5.267954779139178E-7)
            it.assertThat(regressed).`as`("regressed").isFalse()
            it.assertThat(sameDistro).`as`("same distro").isTrue()
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
        val test = ShiftedDistributionRegressionTest(baseline, experiment)

        // when
        val absoluteShift = -test.locationShift
        val relativeShift = -test.percentageShift
        val regressed = test.isExperimentRegressed(0.00)
        val sameDistro = test.equalDistributionsAfterShift

        // then
        plotQuantiles(baseline, experiment)
        assertSoftly {
            it.assertThat(absoluteShift).`as`("absolute shift").isEqualTo(800.0)
            it.assertThat(relativeShift).`as`("relative shift").isEqualTo(1.970670929558996)
            it.assertThat(regressed).`as`("regressed").isTrue()
            it.assertThat(sameDistro).`as`("same distro").isTrue()
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
        val test = ShiftedDistributionRegressionTest(baseline, experiment)

        // when
        val absoluteShift = -test.locationShift
        val relativeShift = -test.percentageShift
        val regressed = test.isExperimentRegressed(0.00)
        val sameDistro = test.equalDistributionsAfterShift

        // then
        plotQuantiles(baseline, experiment)
        assertSoftly {
            it.assertThat(absoluteShift).`as`("absolute shift").isEqualTo(66.57177057231809)
            it.assertThat(relativeShift).`as`("relative shift").isEqualTo(0.16398881624517286)
            it.assertThat(regressed).`as`("regressed").isTrue()
            it.assertThat(sameDistro).`as`("same distro").isFalse()
        }
    }

    /**
     * In a 51% slightly faster vs 49% much slower case, it should be a regression
     */
    @Ignore("https://ecosystem.atlassian.net/browse/JPERF-1297")
    @Test
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
        val test = ShiftedDistributionRegressionTest(baseline, experiment)

        // when
        val absoluteShift = -test.locationShift
        val relativeShift = -test.percentageShift
        val regressed = test.isExperimentRegressed(0.00)
        val sameDistro = test.equalDistributionsAfterShift

        // then
        plotQuantiles(baseline, experiment)
        assertSoftly {
            it.assertThat(absoluteShift).`as`("absolute shift").isCloseTo(slowdown, Offset.offset(500.0))
            it.assertThat(relativeShift).`as`("relative shift").isCloseTo(0.75, Offset.offset(0.01))
            it.assertThat(regressed).`as`("regressed").isTrue()
            it.assertThat(sameDistro).`as`("same distro").isFalse()
        }
    }

    @Test
    @Ignore("percentageShift calculation is fixed in in DistributionComparator where Hodges-Lehmann estimator with pseudo-median is used")
    fun shouldDetectImprovementWhenEveryPercentileBetter() {
        // given
        val baseline =
            this.javaClass.getResource("/real-results/view issue 9.17.0 vs 10.0.0/baseline.csv").readText().lines()
                .map { it.toDouble() }.toDoubleArray()
        val experiment =
            this.javaClass.getResource("/real-results/view issue 9.17.0 vs 10.0.0/experiment.csv").readText().lines()
                .map { it.toDouble() }.toDoubleArray()
        // when
        val test = ShiftedDistributionRegressionTest(baseline, experiment)
        // then
        plotQuantiles(baseline, experiment)
        assertSoftly {
            it.assertThat(test.isExperimentRegressed(0.01)).`as`("isExperimentRegressed").isFalse()
            it.assertThat(test.percentageShift).`as`("").`as`("percentageShift").isEqualTo(0.03941908713692943)
            it.assertThat(test.locationShift).`as`("").`as`("locationShift").isEqualTo(20.0)
            it.assertThat(test.overcomesTolerance(0.01)).`as`("overcomesTolerance").isTrue()
        }
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
        val htmlFile = Files.createTempFile("kebab", ".html")
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
        val result = FakeResults.fastResult
            .actionMetrics.map { it.duration.toMillis() }
            .map { it.toDouble() }.toDoubleArray()

        val percentageShift = ShiftedDistributionRegressionTest(result, result).percentageShift

        assertThat(percentageShift).isEqualTo(0.0)
    }
}
