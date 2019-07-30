package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.EdibleResult

class RelativeNonparametricStabilityJudge(
    private val significance: Double
) {

    constructor() : this(significance = 0.05)

    fun judge(
        actions: List<ActionType<*>>,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): Verdict {
        val actionReports = mutableListOf<ActionReport>()
        actions.forEach { action ->
            val actionReport = judge(
                action,
                baselineResult,
                experimentResult
            )
            actionReports.add(actionReport)
        }

        return Verdict(actionReports)
    }

    private fun judge(
        action: ActionType<*>,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): ActionReportImpl {
        val label = action.label
        val baselineCohort = baselineResult.cohort
        val experimentCohort = experimentResult.cohort
        val reportName = "Stability regression for $label $experimentCohort vs $baselineCohort"
        val reader = ActionMetricsReader()
        val baseline = reader.read(baselineResult.actionMetrics)[label]?.stats?.values
            ?: return ActionReportImpl(
                report = FailedAssertionJUnitReport(reportName, "No action $label results for $baselineCohort"),
                action = action,
                nonExceptional = false
            )
        val experiment = reader.read(experimentResult.actionMetrics)[label]?.stats?.values
            ?: return ActionReportImpl(
                report = FailedAssertionJUnitReport(reportName, "No action $label results for $experimentCohort"),
                action = action,
                nonExceptional = false
            )
        val test = ShiftedDistributionRegressionTest(baseline, experiment, mwAlpha = 0.0, ksAlpha = significance)
        return if (!test.equalDistributionsAfterShift) {
            val message = "[$label] distribution shapes are different at $significance significance level"
            ActionReportImpl(
                report = FailedAssertionJUnitReport(reportName, message),
                action = action,
                nonExceptional = true
            )
        } else {
            ActionReportImpl(
                report = SuccessfulJUnitReport(testName = reportName),
                action = action,
                nonExceptional = false
            )
        }
    }
}
