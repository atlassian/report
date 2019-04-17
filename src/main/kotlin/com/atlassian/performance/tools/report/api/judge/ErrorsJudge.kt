package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.ErrorCriteria
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.Stats
import com.atlassian.performance.tools.report.result.PerformanceStats

class ErrorsJudge {

    @Deprecated(message = "Use the other judge method.")
    fun judge(
        stats: InteractionStats,
        criteria: Map<ActionType<*>, ErrorCriteria>
    ): Verdict = this.judge(
        stats = PerformanceStats.adapt(stats),
        criteria = criteria
    )

    fun judge(
        stats: Stats,
        criteria: Map<ActionType<*>, ErrorCriteria>
    ): Verdict {
        val testReports = mutableListOf<JUnitReport>()

        for ((action, errorCriteria) in criteria) {
            val acceptableErrorCount = errorCriteria.acceptableErrorCount
            val errorCount = stats.errors[action.label] ?: 0

            if (errorCount > acceptableErrorCount) {
                testReports.add(
                    FailedAssertionJUnitReport(
                        testMethodName(action, stats.cohort),
                        "The '${action.label}' action has failed $errorCount times. " +
                            "This is too much, because we only tolerate $acceptableErrorCount such failures. " +
                            "It happened on the '${stats.cohort}' cohort."
                    )
                )
            } else {
                testReports.add(SuccessfulJUnitReport(testMethodName(action, stats.cohort)))
            }
        }

        return Verdict(testReports)
    }

    private fun testMethodName(
        action: ActionType<*>,
        cohort: String
    ) = "Error_count_for_${cohort}_${action.label}"
}
