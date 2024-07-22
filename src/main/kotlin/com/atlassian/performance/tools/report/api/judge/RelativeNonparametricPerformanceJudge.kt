package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.distribution.DistributionComparator
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.junit.FailedActionJunitReport
import com.atlassian.performance.tools.report.toPercentage
import java.util.function.Consumer

class RelativeNonparametricPerformanceJudge private constructor(
    private val significance: Double,
    private val impactHandlers: List<Consumer<LatencyImpact>>
) {

    @Deprecated(
        "Use Builder instead",
        ReplaceWith("RelativeNonparametricPerformanceJudge.Builder().significance(significance).build()")
    )
    constructor(significance: Double) : this(significance, emptyList())

    @Deprecated(
        "Use Builder instead",
        ReplaceWith("RelativeNonparametricPerformanceJudge.Builder().build()")
    )
    constructor() : this(0.05, emptyList())

    fun judge(
        toleranceRatios: Map<ActionType<*>, Float>,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): Verdict {
        val verdict = Verdict.Builder()
        for ((action, toleranceRatio) in toleranceRatios) {
            val actionReport = judge(
                action,
                toleranceRatio,
                baselineResult,
                experimentResult
            )
            verdict.addReport(actionReport.report)
            if (actionReport.nonExceptionalFailure) {
                verdict.addFailedAction(actionReport.action)
            }
        }
        return verdict.build()
    }

    private fun judge(
        action: ActionType<*>,
        toleranceRatio: Float,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): ActionReport {
        val label = action.label
        val baselineCohort = baselineResult.cohort
        val experimentCohort = experimentResult.cohort
        val reportName = "Regression for $label $experimentCohort vs $baselineCohort"
        val reader = ActionMetricsReader()
        val baseline = reader.read(baselineResult.actionMetrics)[label]?.stats?.values
            ?: return ActionReport(
                report = FailedAssertionJUnitReport(reportName, "No action $label results for $baselineCohort"),
                action = action
            )
        val experiment = reader.read(experimentResult.actionMetrics)[label]?.stats?.values
            ?: return ActionReport(
                report = FailedAssertionJUnitReport(reportName, "No action $label results for $experimentCohort"),
                action = action
            )
        val comparison = DistributionComparator.Builder(baseline, experiment)
            .tolerance(toleranceRatio.toDouble())
            .build()
            .compare()
        val impact = LatencyImpact.Builder(
            action,
            comparison.experimentRelativeChange,
            reader.convertToDuration(comparison.experimentShift)
        )
            .relevant(comparison.hasImpact())
            .build()
        impactHandlers.forEach { it.accept(impact) }
        return if (impact.regression) {
            val confidenceLevelPercent = (1.0 - significance).toPercentage(decimalPlaces = 0, includeSign = false)
            val message =
                "There is a regression in [$label] with $confidenceLevelPercent confidence level. Regression is larger than allowed ${
                    toleranceRatio.toPercentage(decimalPlaces = 2)
                } tolerance"
            ActionReport(
                report = FailedActionJunitReport(testName = reportName, assertion = message),
                action = action,
                nonExceptionalFailure = true
            )
        } else {
            ActionReport(
                report = SuccessfulJUnitReport(reportName),
                action = action
            )
        }
    }

    class Builder {
        private var significance: Double = 0.05
        private val impactHandlers: MutableList<Consumer<LatencyImpact>> = mutableListOf()

        fun significance(significance: Double) = apply { this.significance = significance }
        fun handleLatencyImpact(handler: Consumer<LatencyImpact>) = apply { impactHandlers.add(handler) }

        fun build() = RelativeNonparametricPerformanceJudge(
            significance,
            impactHandlers
        )
    }
}

internal data class ActionReport(
    val report: JUnitReport,
    val action: ActionType<*>,
    val nonExceptionalFailure: Boolean = false
)
