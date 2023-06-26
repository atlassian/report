package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.EDIT_ISSUE
import com.atlassian.performance.tools.report.api.result.FakeResults
import com.atlassian.performance.tools.report.junit.FailedActionJunitReport
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration
import java.util.function.Consumer

class RelativeNonparametricPerformanceJudgeTest {

    private val zeroToleranceRatios = FakeResults.actionTypes.associate { it to 0.1f }.toMap()
    private val tested = RelativeNonparametricPerformanceJudge.Builder().build()
        .judge(
            toleranceRatios = zeroToleranceRatios,
            baselineResult = FakeResults.fastResult,
            experimentResult = FakeResults.slowResult
        )

    @Test
    fun shouldVerdictHaveFailedReports() {
        assertThat(tested.reports).hasSize(2)
        assertThat(tested.reports).allSatisfy { !it.successful }
    }

    @Test
    fun shouldFailedReportXmlHaveExpectedMessage() {
        assertThat(tested.reports).hasSize(2)
        tested.reports.first().let { report ->
            assertThat(report).isInstanceOf(FailedActionJunitReport::class.java)
            assertThat((report as FailedActionJunitReport).toXml(testClassName = "does not matter"))
                .contains("There is a regression in [Full Edit Issue] with 95% confidence level. Regression is larger than allowed +10.00% tolerance")
        }
    }

    @Test
    fun shouldReportLatencyImpacts() {
        val impacts = mutableListOf<LatencyImpact>()

        RelativeNonparametricPerformanceJudge.Builder()
            .handleLatencyImpact(Consumer { impacts.add(it) })
            .build()
            .judge(
                toleranceRatios = zeroToleranceRatios,
                baselineResult = FakeResults.fastResult,
                experimentResult = FakeResults.slowResult
            )

        assertThat(impacts).isNotEmpty()
        assertThat(impacts.map { it.action }).contains(EDIT_ISSUE)
        assertThat(impacts.single { it.action == EDIT_ISSUE }).satisfies { editIssueImpact ->
            assertThat(editIssueImpact.relative).isBetween(160.0, 165.0)
            assertThat(editIssueImpact.absolute).isBetween(Duration.ofMinutes(1), Duration.ofMinutes(2))
            assertThat(editIssueImpact.regression).isTrue()
            assertThat(editIssueImpact.signal).isTrue()
        }
    }
}
