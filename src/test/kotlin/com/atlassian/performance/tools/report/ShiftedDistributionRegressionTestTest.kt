package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.report.api.FullTimeline
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.result.LocalRealResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.Test
import java.nio.file.Paths

class ShiftedDistributionRegressionTestTest {

    @Test
    fun shouldBeEqualDistributionsViewIssueAt0_69Significance() {
        val test = testForAction("JIRA-JPT760-JOB1-8", VIEW_ISSUE, ksAlpha = .69)

        assertThat(test.equalDistributionsAfterShift).isTrue()
    }

    @Test
    fun shouldNotBeEqualDistributionsViewIssueAt0_70Significance() {
        val test = testForAction("JIRA-JPT760-JOB1-8", VIEW_ISSUE, ksAlpha = .70)

        assertThat(test.equalDistributionsAfterShift).isFalse()
    }

    @Test
    fun shouldReportCorrectLocationViewIssue() {
        val test = testForAction("JIRA-JPT760-JOB1-8", VIEW_ISSUE)

        assertThat(test.locationShift).isCloseTo(-0.008, Percentage.withPercentage(0.0001))
    }

    @Test
    fun shouldReportRegressionViewBoardAt0_0Tolerance() {
        val test = testForAction("JIRA-JPT760-JOB1-8", VIEW_BOARD)

        assertThat(test.isExperimentRegressed(0.0)).isTrue()
    }

    @Test
    fun shouldNotReportRegressionViewBoardAt0_5Tolerance() {
        val test = testForAction("JIRA-JPT760-JOB1-8", VIEW_BOARD)

        assertThat(test.isExperimentRegressed(0.5)).isFalse()
    }

    @Test
    fun shouldReportCorrectPercentageShiftEditIssue() {
        val test = testForAction("JIRA-JPT760-JOB1-8", EDIT_ISSUE_SUBMIT)

        assertThat(test.percentageShift).isCloseTo(0.018774, Percentage.withPercentage(0.01))
    }

    private fun testForAction(
        path: String,
        action: ActionType<*>,
        mwAlpha: Double = .05,
        ksAlpha: Double = .05
    ): ShiftedDistributionRegressionTest {
        val nanosInSecond = 1e9
        val resultsPath = Paths.get(path)
        val timeline = FullTimeline()
        val edibleAlphaResults = LocalRealResult(resultsPath.resolve("alpha"))
            .loadRaw()
            .prepareForJudgement(timeline)
        val edibleBetaResults = LocalRealResult(resultsPath.resolve("beta"))
            .loadRaw()
            .prepareForJudgement(timeline)
        val label = action.label
        val reader = ActionMetricsReader()
        val baseline = reader.read(edibleAlphaResults.actionMetrics)[label]?.stats!!.values
        val experiment = reader.read(edibleBetaResults.actionMetrics)[label]?.stats!!.values
        baseline.indices.forEach { baseline[it] /= nanosInSecond }
        experiment.indices.forEach { experiment[it] /= nanosInSecond }
        return ShiftedDistributionRegressionTest(baseline, experiment, mwAlpha, ksAlpha)
    }
}
