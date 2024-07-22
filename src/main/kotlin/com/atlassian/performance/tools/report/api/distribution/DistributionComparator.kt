package com.atlassian.performance.tools.report.api.distribution

import com.numericalmethod.suanshu.stats.test.rank.wilcoxon.WilcoxonRankSum
import org.apache.commons.math3.stat.descriptive.rank.Median
import org.apache.commons.math3.stat.ranking.NaNStrategy

class DistributionComparator private constructor(
    private val baseline: DoubleArray,
    private val experiment: DoubleArray,
    /**
     * A percentage by which experiment can be slower/faster than baseline and not considered as a regression/improvement
     */
    private val tolerance: Double,
    private val significance: Double
) {



    /**
     * Performs a one-tailed Mann–Whitney U test to check whether experiment is not slower than the baseline
     *
     * @return true if the experiment is slower than the baseline by more than tolerance, false otherwise
     */
    private fun isExperimentRegressed(baselineMedian: Double): Boolean {
        val mu = -tolerance * baselineMedian
        return WilcoxonRankSum(baseline, experiment, mu).pValue1SidedLess < significance
    }

    private fun isExperimentImproved(baselineMedian: Double): Boolean {
        val mu = -tolerance * baselineMedian
        val wilcoxon = WilcoxonRankSum(experiment, baseline, mu)
        return wilcoxon.pValue1SidedLess < significance
    }

    /**
     * Pseudo-median: the median of the Walsh (pairwise) averages
     */
    private fun pseudoMedian(array: DoubleArray): Double {
        val n = array.size
        val size = n * (n + 1) / 2 - n
        val values = DoubleArray(size)
        var k = 0
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                values[k++] = (array[i] + array[j]) / 2
            }
        }
        return Median().evaluate(values)
    }

    private fun median(func: (xi: Double, yj: Double) -> Double): Double {
        val values = DoubleArray(baseline.size * experiment.size)
        var k = 0
        for (i in baseline.indices) {
            for (j in experiment.indices) {
                values[k++] = func(baseline[i], experiment[j])
            }
        }
        return Median().withNaNStrategy(NaNStrategy.MINIMAL).evaluate(values)
    }

    private fun shift(): Double {
        return median { xi, yj -> yj - xi }
    }

    private fun ratio(): Double {
        return median { xi, yj -> yj / xi }
    }

    /**
     * Calculates the distance between two data sets based on the [Hodges-Lehmann estimator][].
     * [Hodges-Lehmann estimator]: https://en.wikipedia.org/wiki/Hodges%E2%80%93Lehmann_estimator
     * https://aakinshin.net/hodges-lehmann-estimator/
     * https://github.com/AndreyAkinshin/perfolizer/blob/master/src/Perfolizer/Perfolizer/Mathematics/GenericEstimators/HodgesLehmannEstimator.cs
     *
     * Takes into account tolerance which answers the question "is change is big enough to matter?"
     */
    fun compare(): DistributionComparison {
        val experimentShift = shift()
        val baselineMedian = pseudoMedian(baseline)
        val experimentRatio = ratio()
        val isExperimentImproved = isExperimentImproved(baselineMedian)
        val isExperimentRegressed = isExperimentRegressed(baselineMedian)
        val experimentRelativeChange = experimentRatio - 1
        return DistributionComparison(
            experimentRelativeChange = experimentRelativeChange,
            experimentAbsoluteChange = experimentShift,
            isExperimentRegressed = isExperimentRegressed,
            isExperimentImproved = isExperimentImproved
        )
    }

    class Builder(
        private var baseline: DoubleArray,
        private var experiment: DoubleArray
    ) {
        private var significance: Double = 0.05
        private var tolerance: Double = 0.01

        fun significance(significance: Double) = apply { this.significance = significance }
        fun tolerance(tolerance: Double) = apply { this.tolerance = tolerance }
        fun baseline(baseline: DoubleArray) = apply { this.baseline = baseline }
        fun experiment(experiment: DoubleArray) = apply { this.experiment = experiment }

        fun build() = DistributionComparator(
            baseline = baseline,
            experiment = experiment,
            tolerance = tolerance,
            significance = significance
        )

    }
}
