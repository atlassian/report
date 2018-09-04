package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.PerformanceCriteria
import com.atlassian.performance.tools.report.api.StandardTimeline
import com.atlassian.performance.tools.report.api.result.CohortResult
import com.atlassian.performance.tools.workspace.api.TestWorkspace

class MaximumCoverageJudge {

    fun judge(
        baseline: CohortResult,
        experiment: CohortResult,
        report: TestWorkspace,
        criteria: PerformanceCriteria
    ): Verdict {
        val edibleBaseline = baseline.prepareForJudgement(
            StandardTimeline(criteria.virtualUserLoad.total)
        )
        val edibleExperiment = experiment.prepareForJudgement(
            StandardTimeline(criteria.virtualUserLoad.total)
        )
        val results = listOf(edibleBaseline, edibleExperiment)
        return IndependentCohortsJudge().judge(
            results = results,
            criteria = criteria,
            workspace = report
        ) + BaselineComparingJudge().judge(
            baselineStats = edibleBaseline.actionStats,
            experimentStats = edibleExperiment.actionStats,
            performanceCriteria = criteria
        )
    }
}