package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.ActionType
import com.atlassian.performance.tools.report.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.junit.JUnitReport
import com.atlassian.performance.tools.report.junit.SuccessfulJUnitReport

class RelativePerformanceStabilityJudge {

    internal fun judge(
        criteria: Map<ActionType<*>, DispersionCriteria>,
        baselineStats: InteractionStats,
        experimentStats: InteractionStats
    ): Verdict {
        val baselineDispersions = baselineStats.dispersions
        val experimentDispersions = experimentStats.dispersions
        if (baselineDispersions == null || experimentDispersions == null) {
            return Verdict(listOfNotNull(
                judgeMissingResults(baselineStats),
                judgeMissingResults(experimentStats)
            ))
        }
        return Verdict(
            criteria.map { (action, dispersionCriteria) ->
                val label = action.label
                val maxDispersionRegression = dispersionCriteria.maxDispersionDifference
                val regression = experimentDispersions[label]!! - baselineDispersions[label]!!
                val reportName = "Stability regression for $label ${experimentStats.cohort} vs ${baselineStats.cohort}"
                return@map if (regression > maxDispersionRegression) {
                    val message = "$label $regression stability regression overcame $maxDispersionRegression threshold"
                    FailedAssertionJUnitReport(reportName, message)
                } else {
                    SuccessfulJUnitReport(testName = reportName)
                }
            }
        )
    }

    private fun judgeMissingResults(
        stats: InteractionStats
    ): JUnitReport? {
        return if (stats.dispersions == null) {
            FailedAssertionJUnitReport(
                testName = "Stability: ${stats.cohort}",
                assertion = "Stability results are missing"
            )
        } else {
            null
        }
    }
}