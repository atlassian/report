package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.PerformanceCriteria
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.api.result.Stats
import com.atlassian.performance.tools.report.result.PerformanceStats

/**
 * Judges an experiment cohort in relation to a baseline cohort.
 */
class BaselineComparingJudge {

    @Deprecated(message = "Use the other judge method.")
    fun judge(
        performanceCriteria: PerformanceCriteria,
        baselineStats: InteractionStats,
        experimentStats: InteractionStats
    ): Verdict = this.judge(
        performanceCriteria = performanceCriteria,
        baselineStats = PerformanceStats.adapt(baselineStats),
        experimentStats = PerformanceStats.adapt(experimentStats)
    )

    fun judge(
        performanceCriteria: PerformanceCriteria,
        baselineStats: Stats,
        experimentStats: Stats
    ): Verdict {
        return RelativeTypicalPerformanceJudge().judge(performanceCriteria.getCenterCriteria(), baselineStats, experimentStats) +
            RelativePerformanceStabilityJudge().judge(performanceCriteria.getDispersionCriteria(), baselineStats, experimentStats)
    }
}