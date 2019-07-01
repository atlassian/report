package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.PerformanceCriteria
import com.atlassian.performance.tools.report.api.StandardTimeline
import com.atlassian.performance.tools.report.api.result.CohortResult
import com.atlassian.performance.tools.report.api.result.RawCohortResult
import com.atlassian.performance.tools.workspace.api.TestWorkspace

class MaximumCoverageJudge {

    @Suppress("DEPRECATION")
    @Deprecated(message = "Use the other judge method.")
    fun judge(
        baseline: CohortResult,
        experiment: CohortResult,
        report: TestWorkspace,
        criteria: PerformanceCriteria
    ): Verdict = judge(
        baseline = CohortResult.toRawCohortResult(baseline),
        experiment = CohortResult.toRawCohortResult(experiment),
        report = report,
        criteria = criteria
    )

    fun judge(
        baseline: RawCohortResult,
        experiment: RawCohortResult,
        report: TestWorkspace,
        criteria: PerformanceCriteria
    ): Verdict {
        val trimming = criteria.actionCriteria.mapValues { (_, actionCriteria) -> actionCriteria.outlierTrimming }
        val edibleBaseline = baseline.prepareForJudgement(
            StandardTimeline(criteria.virtualUserLoad.total),
            trimming
        )
        val edibleExperiment = experiment.prepareForJudgement(
            StandardTimeline(criteria.virtualUserLoad.total),
            trimming
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
