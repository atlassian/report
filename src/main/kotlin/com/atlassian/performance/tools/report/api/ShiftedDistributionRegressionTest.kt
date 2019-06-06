package com.atlassian.performance.tools.report.api

import com.numericalmethod.suanshu.stats.test.distribution.kolmogorov.KolmogorovSmirnov.Side
import com.numericalmethod.suanshu.stats.test.distribution.kolmogorov.KolmogorovSmirnov2Samples
import com.numericalmethod.suanshu.stats.test.rank.wilcoxon.WilcoxonRankSum
import org.apache.commons.math3.stat.descriptive.rank.Median

/**
 * Provides means for nonparametric comparison of two data sets
 * Uses [SuanShu](http://redmine.numericalmethod.com/projects/public/repository/show/SuanShu-20120606) open source library
 *
 * @baseline baseline durations
 * @experiment experiment durations
 * @param mwAlpha Mann-Whitney significance level
 * @param ksAlpha Kolmogorov-Smirnov significance level
 */
class ShiftedDistributionRegressionTest(
    private val baseline: DoubleArray,
    private val experiment: DoubleArray,
    private val mwAlpha: Double,
    private val ksAlpha: Double
) {

    constructor(
        baseline: DoubleArray,
        experiment: DoubleArray
    ) : this(
        baseline = baseline,
        experiment = experiment,
        mwAlpha = 0.05,
        ksAlpha = 0.05
    )

    private val shift: Double by lazy { hodgesLehmannDistance(baseline, experiment) }
    private val baselineMedian: Double by lazy { Median().evaluate(baseline) }

    /**
     * Make sure equalDistributionsAfterShift is TRUE before accessing this property
     */
    val locationShift: Double by lazy {
        if (!equalDistributionsAfterShift) {
            throw Exception("Distribution shapes are different at $ksAlpha significance level. Location shift is meaningless.")
        }
        return@lazy shift
    }

    /**
     * Location shift as a percentage of baseline's median
     * Make sure equalDistributionsAfterShift is TRUE before accessing this property
     */
    val percentageShift: Double by lazy { locationShift / baselineMedian }

    /**
     * Tests for equality of probability distribution shapes ignoring location
     * Keep in mind that due to limitation of Kolmogorov-Smirnov algorithm, this test may fail to detect difference
     * when sample size is low (<100), but might be too sensitive when sample size is very large
     */
    val equalDistributionsAfterShift: Boolean by lazy {
        val shiftedExperiment = experiment.copyOf()
        shiftedExperiment.indices.forEach { shiftedExperiment[it] += shift }
        return@lazy KolmogorovSmirnov2Samples(baseline, shiftedExperiment, Side.TWO_SIDED).pValue() >= ksAlpha
    }

    /**
     * Performs a one-tailed Mann–Whitney U test to check whether experiment is not slower than the baseline
     *
     * @param tolerance A percentage by which experiment can be slower than baseline and not considered as a regression
     * @return true if the experiment is slower than the baseline by more than tolerance, false otherwise
     */
    fun isExperimentRegressed(tolerance: Double): Boolean {
        val mu = - tolerance * baselineMedian
        return WilcoxonRankSum(baseline, experiment, mu).pValue1SidedLess < mwAlpha
    }

    /**
     * Calculates the distance between two data sets based on the [Hodges-Lehmann estimator][].
     * [Hodges-Lehmann estimator]: https://en.wikipedia.org/wiki/Hodges%E2%80%93Lehmann_estimator
     */
    private fun hodgesLehmannDistance(data1: DoubleArray, data2: DoubleArray): Double {
        val differences = DoubleArray(data1.size * data2.size)
        for (i in data1.indices) {
            for (j in data2.indices) {
                differences[i * data2.size + j] = data1[i] - data2[j]
            }
        }
        return Median().evaluate(differences)
    }
}
