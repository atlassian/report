package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.ActionType
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.api.SampleSizeCriteria
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport

class SampleSizeJudge {

    fun judge(
        stats: InteractionStats,
        criteria: Map<ActionType<*>, SampleSizeCriteria>
    ): Verdict {
        val sampleSizes = stats.sampleSizes ?: return Verdict(listOf(
            FailedAssertionJUnitReport(
                testName = "Sample size of ${stats.cohort}",
                assertion = "Sample size results are missing"
            )
        ))
        val testReports = mutableListOf<JUnitReport>()
        for ((action, sampleCriteria) in criteria) {
            val sampleSize = sampleSizes[action.label]!!
            val minimumSampleSize = sampleCriteria.minimumSampleSize
            if (sampleSize < minimumSampleSize) {
                val message = "The sample size of ${stats.cohort} ${action.label} is $sampleSize," +
                    " which is under the minimum threshold of $minimumSampleSize"
                testReports.add(FailedAssertionJUnitReport(testMethodName(stats, action), message))
            } else {
                testReports.add(SuccessfulJUnitReport(testMethodName(stats, action)))
            }
        }
        return Verdict(testReports)
    }

    private fun testMethodName(
        stats: InteractionStats,
        action: ActionType<*>
    ) = "Sample_size_for_${stats.cohort}_${action.label}"
}