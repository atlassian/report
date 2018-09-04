package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import java.time.Duration

class RelativePerformanceStabilityJudge {

    internal fun judge(
        maxDispersionDifferences: Map<ActionType<*>, Duration>,
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
            maxDispersionDifferences.map { (action, maxDispersionDifference) ->
                val label = action.label
                val regression = experimentDispersions[label]!! - baselineDispersions[label]!!
                val reportName = "Stability regression for $label ${experimentStats.cohort} vs ${baselineStats.cohort}"
                return@map if (regression > maxDispersionDifference) {
                    val message = "$label $regression stability regression overcame $maxDispersionDifference threshold"
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