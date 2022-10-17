package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest

open class ShiftedDistributionRegressionTestProvider {
    open fun get(
        baseline: DoubleArray,
        experiment: DoubleArray,
        mwAlpha: Double,
        ksAlpha: Double
    ): ShiftedDistributionRegressionTest {
        return ShiftedDistributionRegressionTest(
            baseline = baseline,
            experiment = experiment,
            ksAlpha = ksAlpha,
            mwAlpha = mwAlpha
        )
    }
}
