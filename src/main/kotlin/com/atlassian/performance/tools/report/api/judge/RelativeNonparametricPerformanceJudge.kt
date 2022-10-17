package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.DurationData
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.junit.FailedActionJunitReport
import com.atlassian.performance.tools.report.toPercentage

class RelativeNonparametricPerformanceJudge private constructor(
    private val significance: Double,
    private val actionMetricsReader: (metrics: List<ActionMetric>) -> Map<String, DurationData>,
    private val shiftedDistributionRegressionTestProvider: ShiftedDistributionRegressionTestProvider
) {

    class Builder {
        private var significance: Double = 0.05
        private var actionMetricsReader: (metrics: List<ActionMetric>) -> Map<String, DurationData> = { metrics -> ActionMetricsReader().read(metrics) }
        private var shiftedDistributionRegressionTestProvider: ShiftedDistributionRegressionTestProvider = ShiftedDistributionRegressionTestProvider()

        fun significance(significance: Double) = apply { this.significance = significance }
        fun actionMetricsReader(actionMetricsReader: (metrics: List<ActionMetric>) -> Map<String, DurationData>) = apply { this.actionMetricsReader = actionMetricsReader }
        fun shiftedDistributionRegressionTestProvider(shiftedDistributionRegressionTestProvider: ShiftedDistributionRegressionTestProvider) =
            apply { this.shiftedDistributionRegressionTestProvider = shiftedDistributionRegressionTestProvider }

        fun build() = RelativeNonparametricPerformanceJudge(
            significance = significance,
            actionMetricsReader = actionMetricsReader,
            shiftedDistributionRegressionTestProvider = shiftedDistributionRegressionTestProvider
        )
    }

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
        val baseline = actionMetricsReader(baselineResult.actionMetrics)[label]?.statsValues()
            ?: return ActionReport(
                report = FailedAssertionJUnitReport(reportName, "No action $label results for $baselineCohort"),
                action = action
            )
        val experiment = actionMetricsReader(experimentResult.actionMetrics)[label]?.statsValues()
            ?: return ActionReport(
                report = FailedAssertionJUnitReport(reportName, "No action $label results for $experimentCohort"),
                action = action
            )
        val test = shiftedDistributionRegressionTestProvider.get(baseline = baseline, experiment = experiment, mwAlpha = significance, ksAlpha = 0.0)
        return if (test.isExperimentRegressed(toleranceRatio.toDouble())) {
            val confidenceLevelPercent = (1.0 - significance).toPercentage(decimalPlaces = 0, includeSign = false)
            val message =
                "There is a regression in [$label] with $confidenceLevelPercent confidence level. Regression is larger than allowed ${toleranceRatio.toPercentage(decimalPlaces = 2)} tolerance"
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
