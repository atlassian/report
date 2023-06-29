package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.api.result.Stats
import com.atlassian.performance.tools.report.result.PerformanceStats
import com.atlassian.performance.tools.report.toPercentage
import java.util.function.Consumer
import kotlin.math.absoluteValue

class RelativeTypicalPerformanceJudge private constructor(
    private val impactHandlers: List<Consumer<LatencyImpact>>
) {

    @Deprecated(
        "Use Builder instead",
        ReplaceWith("RelativeTypicalPerformanceJudge.Builder().build()")
    )
    constructor() : this(emptyList())

    @Deprecated(message = "Use the other judge method.")
    fun judge(
        toleranceRatios: Map<ActionType<*>, Float>,
        baselineStats: InteractionStats,
        experimentStats: InteractionStats
    ): Verdict = this.judge(
        toleranceRatios = toleranceRatios,
        baselineStats = PerformanceStats.adapt(baselineStats),
        experimentStats = PerformanceStats.adapt(experimentStats)
    )

    fun judge(
        toleranceRatios: Map<ActionType<*>, Float>,
        baselineStats: Stats,
        experimentStats: Stats
    ): Verdict {
        val verdict = Verdict.Builder()
        for ((action, toleranceRatio) in toleranceRatios) {
            verdict.addReport(
                judge(
                    action,
                    toleranceRatio,
                    baselineStats,
                    experimentStats
                )
            )
        }
        return verdict.build()
    }

    private fun judge(
        action: ActionType<*>,
        toleranceRatio: Float,
        baselineStats: Stats,
        experimentStats: Stats
    ): JUnitReport {
        val label = action.label
        val baselineCohort = baselineStats.cohort
        val experimentCohort = experimentStats.cohort
        val reportName = "Regression for $label $experimentCohort vs $baselineCohort"
        val baselineCenter = baselineStats.locations[label]
            ?: return FailedAssertionJUnitReport(reportName, "No action $label results for $baselineCohort")
        val experimentCenter = experimentStats.locations[label]
            ?: return FailedAssertionJUnitReport(reportName, "No action $label results for $experimentCohort")
        val absoluteDiff = experimentCenter - baselineCenter
        val relativeDiff = absoluteDiff.toNanos().toDouble() / baselineCenter.toNanos().toDouble()
        val impact = LatencyImpact.Builder(action, relativeDiff, absoluteDiff)
            .noise(relativeDiff.absoluteValue < toleranceRatio)
            .build()
        impactHandlers.forEach { it.accept(impact) }
        return if (impact.regression) {
            val regressionDescription = "$label ${relativeDiff.toPercentage(decimalPlaces = 0)} typical performance regression"
            val toleranceDescription = "${toleranceRatio.toPercentage(decimalPlaces = 0)} tolerance"
            val message = "$regressionDescription overcame $toleranceDescription"
            FailedAssertionJUnitReport(reportName, message)
        } else {
            SuccessfulJUnitReport(reportName)
        }
    }

    class Builder {

        private val impactHandlers: MutableList<Consumer<LatencyImpact>> = mutableListOf()

        fun handleLatencyImpact(handler: Consumer<LatencyImpact>) = apply { impactHandlers.add(handler) }

        fun build() = RelativeTypicalPerformanceJudge(impactHandlers)
    }
}
