package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.Stats
import com.atlassian.performance.tools.report.result.PerformanceStats
import java.time.Duration

class RelativePerformanceStabilityJudge {

    @Deprecated("Use the other judge method")
    internal fun judge(
        maxDispersionDifferences: Map<ActionType<*>, Duration>,
        baselineStats: InteractionStats,
        experimentStats: InteractionStats
    ): Verdict = this.judge(
        maxDispersionDifferences = maxDispersionDifferences,
        baselineStats = PerformanceStats.adapt(baselineStats),
        experimentStats = PerformanceStats.adapt(experimentStats)
    )

    internal fun judge(
        maxDispersionDifferences: Map<ActionType<*>, Duration>,
        baselineStats: Stats,
        experimentStats: Stats
    ): Verdict {
        val baselineDispersions = baselineStats.dispersions
        val experimentDispersions = experimentStats.dispersions
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
}