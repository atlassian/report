package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.Stats
import com.atlassian.performance.tools.report.result.PerformanceStats
import com.atlassian.performance.tools.report.toPercentage

class RelativeTypicalPerformanceJudge {

    @Deprecated(message = "Use the other judge method.")
    fun judge(
        toleranceRatios: Map<ActionType<*>, Float>,
        baselineStats: InteractionStats,
        experimentStats: InteractionStats
    ): Verdict = this.judge(
        toleranceRatios = toleranceRatios,
        baselineStats = PerformanceStats.adapt(baselineStats),
        experimentStats = PerformanceStats.adapt(experimentStats)
    )

    fun judge(
        toleranceRatios: Map<ActionType<*>, Float>,
        baselineStats: Stats,
        experimentStats: Stats
    ): Verdict {
        val testReports = mutableListOf<JUnitReport>()
        for ((action, toleranceRatio) in toleranceRatios) {
            testReports.add(
                judge(
                    action,
                    toleranceRatio,
                    baselineStats,
                    experimentStats
                )
            )
        }
        return Verdict.Builder(reports = testReports).build()
    }

    private fun judge(
        action: ActionType<*>,
        toleranceRatio: Float,
        baselineStats: Stats,
        experimentStats: Stats
    ): JUnitReport {
        val label = action.label
        val baselineCohort = baselineStats.cohort
        val experimentCohort = experimentStats.cohort
        val reportName = "Regression for $label $experimentCohort vs $baselineCohort"
        val baselineCenter = baselineStats.locations[label]
            ?: return FailedAssertionJUnitReport(reportName, "No action $label results for $baselineCohort")
        val experimentCenter = experimentStats.locations[label]
            ?: return FailedAssertionJUnitReport(reportName, "No action $label results for $experimentCohort")
        val regression = (experimentCenter.toNanos().toFloat() / baselineCenter.toNanos().toFloat()) - 1.00f
        return if (regression > toleranceRatio) {
            val regressionDescription = "$label ${regression.toPercentage(decimalPlaces = 0)} typical performance regression"
            val toleranceDescription = "${toleranceRatio.toPercentage(decimalPlaces = 0)} tolerance"
            val message = "$regressionDescription overcame $toleranceDescription"
            FailedAssertionJUnitReport(reportName, message)
        } else {
            SuccessfulJUnitReport(reportName)
        }
    }
}
