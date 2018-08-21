package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.workspace.api.TestWorkspace

class MaximumCoverageJudge {

    fun judge(
        baseline: CohortResult,
        experiment: CohortResult,
        report: TestWorkspace,
        criteria: PerformanceCriteria
    ): Verdict {
        val edibleBaseline = baseline.prepareForJudgement(
            criteria,
            StandardTimeline(criteria.loadProfile)
        )
        val edibleExperiment = experiment.prepareForJudgement(
            criteria,
            StandardTimeline(criteria.loadProfile)
        )
        return IndependentCohortsJudge().judge(
            results = listOf(edibleBaseline, edibleExperiment),
            report = report
        ) + BaselineComparingJudge().judge(
            baseline = edibleBaseline,
            experiment = edibleExperiment
        )
    }
}