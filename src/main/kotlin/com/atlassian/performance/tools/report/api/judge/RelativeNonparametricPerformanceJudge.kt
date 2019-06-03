package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.EdibleResult

class RelativeNonparametricPerformanceJudge {

    private val significance = 0.05

    fun judge(
        toleranceRatios: Map<ActionType<*>, Float>,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): Verdict {
        val testReports = mutableListOf<JUnitReport>()
        for ((action, toleranceRatio) in toleranceRatios) {
            testReports.add(
                judge(
                    action,
                    toleranceRatio,
                    baselineResult,
                    experimentResult
                )
            )
        }
        return Verdict(testReports)
    }

    private fun judge(
        action: ActionType<*>,
        toleranceRatio: Float,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): JUnitReport {
        val label = action.label
        val baselineCohort = baselineResult.cohort
        val experimentCohort = experimentResult.cohort
        val reportName = "Regression for $label $experimentCohort vs $baselineCohort"
        val reader = ActionMetricsReader()
        val baseline = reader.read(baselineResult.actionMetrics)[label]?.stats?.values
            ?: return FailedAssertionJUnitReport(reportName, "No action $label results for $baselineCohort")
        val experiment = reader.read(experimentResult.actionMetrics)[label]?.stats?.values
            ?: return FailedAssertionJUnitReport(reportName, "No action $label results for $experimentCohort")
        val test = ShiftedDistributionRegressionTest(baseline, experiment, mwAlpha = significance, ksAlpha = 0.0)
        return if (test.isExperimentRegressed(toleranceRatio.toDouble())) {
            val message = "Regression in [$label] is larger than allowed ${toleranceRatio.toPercentage()} tolerance at $significance significance level"
            FailedAssertionJUnitReport(reportName, message)
        } else {
            SuccessfulJUnitReport(reportName)
        }
    }

    private fun Float.toPercentage(): String = "%+.0f%%".format(this * 100)
}
