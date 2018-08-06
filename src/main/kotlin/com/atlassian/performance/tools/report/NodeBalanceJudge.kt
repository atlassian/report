package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.report.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.junit.JUnitReport
import com.atlassian.performance.tools.report.junit.SuccessfulJUnitReport

class NodeBalanceJudge(
    private val criteria: PerformanceCriteria,
    private val cohort: String
) {
    fun judge(nodeCounts: Map<String, Int>): Verdict {
        if (nodeCounts.size != criteria.nodes) {
            return Verdict(
                listOf<JUnitReport>(
                    FailedAssertionJUnitReport(
                        testMethodName(cohort),
                        "Results are not available for some nodes. "
                            + "There should be ${criteria.nodes} results but is ${nodeCounts.size}. "
                            + "See node's distribution : $nodeCounts"
                    )
                )
            )
        }

        val maxVirtualUsers = nodeCounts.maxBy { entry -> entry.value }!!
        val minVirtualUsers = nodeCounts.minBy { entry -> entry.value }!!
        val diff = maxVirtualUsers.value - minVirtualUsers.value
        if (diff <= criteria.maxVirtualUsersImbalance) {
            return Verdict(
                listOf<JUnitReport>(
                    SuccessfulJUnitReport(testMethodName(cohort))
                )
            )
        } else {
            return Verdict(
                listOf<JUnitReport>(
                    FailedAssertionJUnitReport(
                        testMethodName(cohort),
                        "More virtual users were testing one node (${maxVirtualUsers.key}) than another (${minVirtualUsers.key}). " +
                            "See node's distribution : $nodeCounts"
                    )
                )
            )
        }
    }

    private fun testMethodName(cohort: String) = "Node_distribution_for_$cohort"
}
