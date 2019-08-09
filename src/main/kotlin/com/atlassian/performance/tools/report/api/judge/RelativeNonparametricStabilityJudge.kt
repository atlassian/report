package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.junit.FailedActionJunitReport

class RelativeNonparametricStabilityJudge(
    private val significance: Double
) {

    constructor() : this(significance = 0.05)

    fun judge(
        actions: List<ActionType<*>>,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): Verdict {
        val testReports = mutableListOf<JUnitReport>()
        val failedActions = mutableListOf<ActionType<*>>()
        actions.forEach { action ->
            val actionReport = judge(
                action,
                baselineResult,
                experimentResult
            )
            testReports.add(actionReport.report)
            if (actionReport.nonExceptionalFailure) {
                failedActions.add(actionReport.action)
            }
        }

        return Verdict(
            reports = testReports,
            failedActions = failedActions
        )
    }

    private fun judge(
        action: ActionType<*>,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): ActionReport {
        val label = action.label
        val baselineCohort = baselineResult.cohort
        val experimentCohort = experimentResult.cohort
        val reportName = "Stability regression for $label $experimentCohort vs $baselineCohort"
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
        val test = ShiftedDistributionRegressionTest(baseline, experiment, mwAlpha = 0.0, ksAlpha = significance)
        return if (!test.equalDistributionsAfterShift) {
            val message = "[$label] distribution shapes are different at $significance significance level"
            ActionReport(
                report = FailedActionJunitReport(reportName, message),
                action = action,
                nonExceptionalFailure = true
            )
        } else {
            ActionReport(
                report = SuccessfulJUnitReport(testName = reportName),
                action = action
            )
        }
    }
}
