package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.junit.FailedActionJunitReport
import com.atlassian.performance.tools.report.toPercentage

class RelativeNonparametricPerformanceJudge(
    private val significance: Double
) {

    constructor() : this(significance = 0.05)

    fun judge(
        toleranceRatios: Map<ActionType<*>, Float>,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): Verdict {
        val verdict = Verdict.Builder()
        for ((action, toleranceRatio) in toleranceRatios) {
            val actionReport = judge(
                action,
                toleranceRatio,
                baselineResult,
                experimentResult
            )
            verdict.addReport(actionReport.report)
            if (actionReport.nonExceptionalFailure) {
                verdict.addFailedAction(actionReport.action)
            }
        }
        return verdict.build()
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
            val confidenceLevelPercent = (1.0 - significance).toPercentage(decimalPlaces = 0, includeSign = false)
            val message = "There is a regression in [$label] with $confidenceLevelPercent confidence level. Regression is larger than allowed ${toleranceRatio.toPercentage(decimalPlaces = 2)} tolerance"
            ActionReport(
                report = FailedActionJunitReport(testName = reportName, assertion = message),
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

}

internal data class ActionReport(
    val report: JUnitReport,
    val action: ActionType<*>,
    val nonExceptionalFailure: Boolean = false
)
