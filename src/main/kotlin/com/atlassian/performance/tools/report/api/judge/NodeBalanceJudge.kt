package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.PerformanceCriteria
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport

class NodeBalanceJudge(
    private val criteria: PerformanceCriteria,
    private val cohort: String
) {
    fun judge(nodeCounts: Map<String, Int>): Verdict {
        if (nodeCounts.size != criteria.nodes) {
            return Verdict.Builder()
                .addReport(
                    FailedAssertionJUnitReport(
                        testName = testMethodName(cohort),
                        assertion = "Results are not available for some nodes. "
                            + "There should be ${criteria.nodes} results but is ${nodeCounts.size}. "
                            + "See node's distribution : $nodeCounts"
                    )
                )
                .build()
        }

        val maxVirtualUsers = nodeCounts.maxBy { entry -> entry.value }!!
        val minVirtualUsers = nodeCounts.minBy { entry -> entry.value }!!
        val diff = maxVirtualUsers.value - minVirtualUsers.value
        return if (diff <= criteria.maxVirtualUsersImbalance) {
            Verdict.Builder()
                .addReport(SuccessfulJUnitReport(testMethodName(cohort)))
                .build()
        } else {
            Verdict.Builder()
                .addReport(
                    FailedAssertionJUnitReport(
                        testName = testMethodName(cohort),
                        assertion = "More virtual users were testing one node (${maxVirtualUsers.key}) than another (${minVirtualUsers.key}). " +
                                "See node's distribution : $nodeCounts"
                    )
                )
                .build()
        }
    }

    private fun testMethodName(cohort: String) = "Node_distribution_for_$cohort"
}
