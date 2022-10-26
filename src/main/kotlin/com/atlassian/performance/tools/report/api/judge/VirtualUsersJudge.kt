package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.PerformanceCriteria
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport

class VirtualUsersJudge(
    private val criteria: PerformanceCriteria
) {
    fun judge(
        nodeCounts: Map<String, Int>,
        cohort: String
    ): Verdict {
        val expected = criteria.virtualUserLoad.virtualUsers
        val active = nodeCounts.values.sum()
        val report = if (expected - active <= criteria.maxInactiveVirtualUsers) {
            SuccessfulJUnitReport(testMethodName(cohort))
        } else {
            FailedAssertionJUnitReport(
                testMethodName(cohort),
                "$expected virtual users were expected, but only $active "
                    + "logged in to the JIRA. It's below maxInactiveVirtualUsers criteria (${criteria.maxInactiveVirtualUsers})"
            )
        }
        return Verdict.Builder()
            .addReport(report)
            .build()
    }

    private fun testMethodName(
        cohort: String
    ) = "Active_virtual_users_count_for_$cohort"
}
