package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport

class RelativeTypicalPerformanceJudge {

    fun judge(
        toleranceRatios: Map<ActionType<*>, Float>,
        baselineStats: InteractionStats,
        experimentStats: InteractionStats
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
        return Verdict(testReports)
    }

    private fun judge(
        action: ActionType<*>,
        toleranceRatio: Float,
        baselineStats: InteractionStats,
        experimentStats: InteractionStats
    ): JUnitReport {
        val label = action.label
        val baselineCohort = baselineStats.cohort
        val experimentCohort = experimentStats.cohort
        val reportName = "Regression for $label $experimentCohort vs $baselineCohort"
        val baselineCenter = baselineStats.centers?.get(label)
            ?: return FailedAssertionJUnitReport(reportName, "No action $label results for $baselineCohort")
        val experimentCenter = experimentStats.centers?.get(label)
            ?: return FailedAssertionJUnitReport(reportName, "No action $label results for $experimentCohort")
        val regression = (experimentCenter.toNanos().toFloat() / baselineCenter.toNanos().toFloat()) - 1.00f
        return if (regression > toleranceRatio) {
            val regressionDescription = "$label ${regression.toPercentage()} typical performance regression"
            val toleranceDescription = "${toleranceRatio.toPercentage()} tolerance"
            val message = "$regressionDescription overcame $toleranceDescription"
            FailedAssertionJUnitReport(reportName, message)
        } else {
            SuccessfulJUnitReport(reportName)
        }
    }
}

private fun Float.toPercentage(): String = "%+.0f%%".format(this * 100)