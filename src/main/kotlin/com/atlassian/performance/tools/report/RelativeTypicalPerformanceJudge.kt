package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.ActionType
import com.atlassian.performance.tools.report.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.junit.JUnitReport
import com.atlassian.performance.tools.report.junit.SuccessfulJUnitReport

class RelativeTypicalPerformanceJudge {

    fun judge(
        criteria: Map<ActionType<*>, CenterCriteria>,
        baselineStats: InteractionStats,
        experimentStats: InteractionStats
    ): Verdict {
        val baselineCenters = baselineStats.centers
        val experimentCenters = experimentStats.centers
        if (baselineCenters == null || experimentCenters == null) {
            return Verdict(listOfNotNull(
                judgeMissingResults(baselineStats),
                judgeMissingResults(experimentStats)
            ))
        }
        val testReports = mutableListOf<JUnitReport>()
        for ((action, centerCriteria) in criteria) {
            val baselineCenter = baselineCenters[action.label]!!
            val experimentCenter = experimentCenters[action.label]!!
            val regression = (experimentCenter.toNanos().toFloat() / baselineCenter.toNanos().toFloat()) - 1.00f
            val regressionDescription = "${action.label} ${regression.toPercentage()} typical performance regression"
            val toleranceDescription = "${centerCriteria.toleranceRatio.toPercentage()} tolerance"
            val reportName = "Regression for ${action.label} ${experimentStats.cohort} vs ${baselineStats.cohort}"
            if (regression > centerCriteria.toleranceRatio) {
                val message = "$regressionDescription overcame $toleranceDescription"
                testReports.add(FailedAssertionJUnitReport(reportName, message))
            } else {
                testReports.add(SuccessfulJUnitReport(reportName))
            }
        }
        return Verdict(testReports)
    }

    private fun judgeMissingResults(
        stats: InteractionStats
    ): JUnitReport? {
        return if (stats.centers == null) {
            FailedAssertionJUnitReport(
                testName = "Latency: ${stats.cohort}",
                assertion = "Latency results are missing"
            )
        } else {
            null
        }
    }
}

private fun Float.toPercentage(): String = "%+.0f%%".format(this * 100)