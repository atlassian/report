package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.api.SampleSizeCriteria
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.Stats
import com.atlassian.performance.tools.report.result.PerformanceStats

class SampleSizeJudge {

    @Deprecated("Use the other judge method.")
    fun judge(
        stats: InteractionStats,
        criteria: Map<ActionType<*>, SampleSizeCriteria>
    ): Verdict = this.judge(
        stats = PerformanceStats.adapt(stats),
        criteria = criteria
    )

    fun judge(
        stats: Stats,
        criteria: Map<ActionType<*>, SampleSizeCriteria>
    ): Verdict {
        val sampleSizes = stats.sampleSizes
        val testReports = mutableListOf<JUnitReport>()
        for ((action, sampleCriteria) in criteria) {
            val sampleSize = sampleSizes[action.label] ?: 0
            val minimumSampleSize = sampleCriteria.minimumSampleSize
            if (sampleSize < minimumSampleSize) {
                val message = "The sample size of ${stats.cohort} ${action.label} is $sampleSize," +
                    " which is under the minimum threshold of $minimumSampleSize"
                testReports.add(FailedAssertionJUnitReport(testMethodName(stats.cohort, action), message))
            } else {
                testReports.add(SuccessfulJUnitReport(testMethodName(stats.cohort, action)))
            }
        }
        return Verdict.Builder(reports = testReports).build()
    }

    private fun testMethodName(
        cohort: String,
        action: ActionType<*>
    ) = "Sample_size_for_${cohort}_${action.label}"
}