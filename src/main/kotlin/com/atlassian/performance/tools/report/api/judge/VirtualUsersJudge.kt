package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.PerformanceCriteria
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport

class VirtualUsersJudge(
    private val criteria: PerformanceCriteria
) {
    fun judge(
        nodeCounts: Map<String, Int>,
        cohort: String
    ): Verdict {
        val expected = criteria.loadProfile.maxExpectedVirtualUsers()
        val active = nodeCounts.values.sum()
        if (expected - active <= criteria.maxInactiveVirtualUsers) {
            return Verdict(
                listOf<JUnitReport>(
                    SuccessfulJUnitReport(testMethodName(cohort)))
            )
        } else {
            return Verdict(
                listOf<JUnitReport>(
                    FailedAssertionJUnitReport(
                        testMethodName(cohort),
                        "$expected virtual users were expected, but only $active "
                            + "logged in to the JIRA. It's below maxInactiveVirtualUsers criteria (${criteria.maxInactiveVirtualUsers})"
                    )
                )
            )
        }
    }

    private fun testMethodName(
        cohort: String
    ) = "Active_virtual_users_count_for_$cohort"
}