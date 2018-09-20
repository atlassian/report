package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.FullReport
import com.atlassian.performance.tools.report.api.PerformanceCriteria
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.workspace.api.TestWorkspace

/**
 * Judges each cohort result in absolute terms, ie. not relative to other cohorts.
 */
class IndependentCohortsJudge {

    fun judge(
        results: List<EdibleResult>,
        criteria: PerformanceCriteria,
        workspace: TestWorkspace
    ): Verdict = judge(
        results
            .map { it to criteria }
            .toMap(),
        workspace
    )

    fun judge(
        results: Map<EdibleResult, PerformanceCriteria>,
        workspace: TestWorkspace
    ): Verdict {
        FullReport().dump(
            results = results.map { (result, _) -> result },
            workspace = workspace
        )
        return results
            .map { (result, criteria) -> judge(result, criteria) }
            .reduce(Verdict::plus)
    }

    private fun judge(
        result: EdibleResult,
        criteria: PerformanceCriteria
    ): Verdict {
        val cohort = result.cohort
        val stats = result.actionStats
        val nodeDistribution = result.nodeDistribution
        val failureVerdict = FailureJudge().judge(result.failure)
        return if (failureVerdict.positive) {
            failureVerdict +
                SampleSizeJudge().judge(stats, criteria.getSampleSizeCriteria()) +
                ErrorsJudge().judge(stats, criteria.getErrorCriteria()) +
                NodeBalanceJudge(criteria, cohort).judge(nodeDistribution) +
                VirtualUsersJudge(criteria).judge(nodeDistribution, cohort)
        } else {
            failureVerdict
        }
    }
}