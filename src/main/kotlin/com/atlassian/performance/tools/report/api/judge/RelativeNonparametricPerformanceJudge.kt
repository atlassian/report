package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.EdibleResult

class RelativeNonparametricPerformanceJudge(
    private val significance: Double
) {

    constructor() : this(significance = 0.05)

    fun judge(
        toleranceRatios: Map<ActionType<*>, Float>,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): Verdict {
        val actionReports = mutableListOf<ActionReport>()
        for ((action, toleranceRatio) in toleranceRatios) {
            val actionReport = judge(
                action,
                toleranceRatio,
                baselineResult,
                experimentResult
            )
            actionReports.add(actionReport)
        }
        return Verdict(actionReports)
    }

    private fun judge(
        action: ActionType<*>,
        toleranceRatio: Float,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): ActionReport {
        val label = action.label
        val baselineCohort = baselineResult.cohort
        val experimentCohort = experimentResult.cohort
        val reportName = "Regression for $label $experimentCohort vs $baselineCohort"
        val reader = ActionMetricsReader()
        val baseline = reader.read(baselineResult.actionMetrics)[label]?.stats?.values
            ?: return ActionReport(
                report = FailedAssertionJUnitReport(reportName, "No action $label results for $baselineCohort"),
                action = action,
                critical = true
            )
        val experiment = reader.read(experimentResult.actionMetrics)[label]?.stats?.values
            ?: return ActionReport(
                report = FailedAssertionJUnitReport(reportName, "No action $label results for $experimentCohort"),
                action = action,
                critical = true
            )
        val test = ShiftedDistributionRegressionTest(baseline, experiment, mwAlpha = significance, ksAlpha = 0.0)
        return if (test.isExperimentRegressed(toleranceRatio.toDouble())) {
            val message = "Regression in [$label] is larger than allowed ${toleranceRatio.toPercentage()} tolerance at $significance significance level"
            ActionReport(
                report = FailedAssertionJUnitReport(reportName, message),
                action = action,
                critical = false
            )
        } else {
            ActionReport(
                report = SuccessfulJUnitReport(reportName),
                action = action,
                critical = false
            )
        }
    }

    private fun Float.toPercentage(): String = "%+.2f%%".format(this * 100)
}
