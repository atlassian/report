package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
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
        val testReports = mutableListOf<JUnitReport>()
        val failedActions = mutableListOf<ActionType<*>>()
        for ((action, toleranceRatio) in toleranceRatios) {
            val actionReport = judge(
                action,
                toleranceRatio,
                baselineResult,
                experimentResult
            )
            testReports.add(actionReport.report)
            if (actionReport.nonExceptionalFailure) {
                failedActions.add(actionReport.action)
            }
        }
        return Verdict(testReports, failedActions)
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
                action = action
            )
        val experiment = reader.read(experimentResult.actionMetrics)[label]?.stats?.values
            ?: return ActionReport(
                report = FailedAssertionJUnitReport(reportName, "No action $label results for $experimentCohort"),
                action = action
            )
        val test = ShiftedDistributionRegressionTest(baseline, experiment, mwAlpha = significance, ksAlpha = 0.0)
        return if (test.isExperimentRegressed(toleranceRatio.toDouble())) {
            val message = "Regression in [$label] is larger than allowed ${toleranceRatio.toPercentage()} tolerance at $significance significance level"
            ActionReport(
                report = FailedAssertionJUnitReport(reportName, message),
                action = action,
                nonExceptionalFailure = true
            )
        } else {
            ActionReport(
                report = SuccessfulJUnitReport(reportName),
                action = action
            )
        }
    }

    private fun Float.toPercentage(): String = "%+.2f%%".format(this * 100)
}

internal data class ActionReport(
    val report: JUnitReport,
    val action: ActionType<*>,
    val nonExceptionalFailure: Boolean = false
)
