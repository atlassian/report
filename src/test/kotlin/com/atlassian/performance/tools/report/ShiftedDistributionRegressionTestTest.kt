package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.report.api.FullTimeline
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.Timeline
import com.atlassian.performance.tools.report.api.result.LocalRealResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.Test
import java.nio.file.Paths

class ShiftedDistributionRegressionTestTest {

    private val resultsPath = Paths.get("JIRA-JPT760-JOB1-8")
    private val rawAlphaResults = LocalRealResult(resultsPath.resolve("alpha")).loadRaw()
    private val rawBetaResults = LocalRealResult(resultsPath.resolve("beta")).loadRaw()
    private val timeline: Timeline = FullTimeline()
    private val edibleAlphaResults = rawAlphaResults.prepareForJudgement(timeline)
    private val edibleBetaResults = rawBetaResults.prepareForJudgement(timeline)

    @Test
    fun shouldBeEqualDistributionsViewIssueAt0_69Significance() {
        val test = testForAction(VIEW_ISSUE, ksAlpha = .69)

        assertThat(test.equalDistributionsAfterShift).isTrue()
    }

    @Test
    fun shouldNotBeEqualDistributionsViewIssueAt0_70Significance() {
        val test = testForAction(VIEW_ISSUE, ksAlpha = .70)

        assertThat(test.equalDistributionsAfterShift).isFalse()
    }

    @Test
    fun shouldReportCorrectLocationViewIssue() {
        val test = testForAction(VIEW_ISSUE)

        assertThat(test.locationShift).isCloseTo(-8000000.0, Percentage.withPercentage(0.0001))
    }

    @Test
    fun shouldReportRegressionViewBoardAt0_0Tolerance() {
        val test = testForAction(VIEW_BOARD)

        assertThat(test.isExperimentRegressed(0.0)).isTrue()
    }

    @Test
    fun shouldNotReportRegressionViewBoardAt0_5Tolerance() {
        val test = testForAction(VIEW_BOARD)

        assertThat(test.isExperimentRegressed(0.5)).isFalse()
    }

    @Test
    fun shouldReportCorrectPercentageShiftEditIssue() {
        val test = testForAction(EDIT_ISSUE_SUBMIT)

        assertThat(test.percentageShift).isCloseTo(0.018774, Percentage.withPercentage(0.01))
    }

    private fun testForAction(
        action: ActionType<*>,
        mwAlpha: Double = .05,
        ksAlpha: Double = .05
    ): ShiftedDistributionRegressionTest {
        val label = action.label
        val reader = ActionMetricsReader()
        val baseline = reader.read(edibleAlphaResults.actionMetrics)[label]?.stats!!.values
        val experiment = reader.read(edibleBetaResults.actionMetrics)[label]?.stats!!.values
        return ShiftedDistributionRegressionTest(baseline, experiment, mwAlpha, ksAlpha)
    }
}
