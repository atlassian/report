package com.atlassian.performance.tools.report

/**
 * Judges an experiment cohort in relation to a baseline cohort.
 */
class BaselineComparingJudge {

    fun judge(
        baseline: EdibleResult,
        experiment: EdibleResult
    ): Verdict {
        val baselineStats = baseline.actionStats
        val experimentStats = experiment.actionStats
        val criteria = baseline.criteria
        return RelativeTypicalPerformanceJudge().judge(criteria.getCenterCriteria(), baselineStats, experimentStats) +
            RelativePerformanceStabilityJudge().judge(criteria.getDispersionCriteria(), baselineStats, experimentStats)
    }
}