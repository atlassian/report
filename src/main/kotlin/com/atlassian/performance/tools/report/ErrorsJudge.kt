package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.ActionType
import com.atlassian.performance.tools.report.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.junit.JUnitReport
import com.atlassian.performance.tools.report.junit.SuccessfulJUnitReport

class ErrorsJudge {

    fun judge(
        stats: InteractionStats,
        criteria: Map<ActionType<*>, ErrorCriteria>
    ): Verdict {
        val errors = stats.errors ?: return Verdict(listOf(
            FailedAssertionJUnitReport(
                testName = "Error count of ${stats.cohort}",
                assertion = "Error count results are missing"
            )
        ))
        val testReports = mutableListOf<JUnitReport>()

        for ((action, errorCriteria) in criteria) {
            val acceptableErrorCount = errorCriteria.acceptableErrorCount
            val errorCount = errors[action.label] ?: 0

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
