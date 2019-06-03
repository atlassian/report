package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import com.atlassian.performance.tools.report.api.result.EdibleResult

class RelativeNonparametricStabilityJudge {

    private val significance = 0.05

    fun judge(
        actions: List<ActionType<*>>,
        baselineResult: EdibleResult,
        experimentResult: EdibleResult
    ): Verdict {
        return Verdict(
            actions.map { action ->
                val label = action.label
                val baselineCohort = baselineResult.cohort
                val experimentCohort = experimentResult.cohort
                val reportName = "Stability regression for $label $experimentCohort vs $baselineCohort"
                val reader = ActionMetricsReader()
                val baseline = reader.read(baselineResult.actionMetrics)[label]?.stats?.values
                    ?: return@map FailedAssertionJUnitReport(reportName, "No action $label results for $baselineCohort")
                val experiment = reader.read(experimentResult.actionMetrics)[label]?.stats?.values
                    ?: return@map FailedAssertionJUnitReport(reportName, "No action $label results for $experimentCohort")
                val test = ShiftedDistributionRegressionTest(baseline, experiment, mwAlpha = 0.0, ksAlpha = significance)
                return@map if (!test.equalDistributionsAfterShift) {
                    val message = "[$label] distribution shapes are different at $significance significance level"
                    FailedAssertionJUnitReport(reportName, message)
                } else {
                    SuccessfulJUnitReport(testName = reportName)
                }
            }
        )
    }
}
