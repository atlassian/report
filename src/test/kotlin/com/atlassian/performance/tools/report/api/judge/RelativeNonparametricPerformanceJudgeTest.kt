package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.EDIT_ISSUE
import com.atlassian.performance.tools.report.api.result.FakeResults
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration
import java.util.function.Consumer

class RelativeNonparametricPerformanceJudgeTest {

    @Test
    fun shouldJudgeRegression() {
        // given
        val zeroToleranceRatios = FakeResults.actionTypes.associate { it to 0.1f }.toMap()
        val impacts = mutableListOf<LatencyImpact>()

        // when
        val verdict = RelativeNonparametricPerformanceJudge.Builder()
            .handleLatencyImpact(Consumer { impacts.add(it) })
            .build()
            .judge(
                toleranceRatios = zeroToleranceRatios,
                baselineResult = FakeResults.fastResult,
                experimentResult = FakeResults.slowResult
            )

        // then
        assertThat(impacts).isNotEmpty()
        assertThat(impacts.map { it.action }).contains(EDIT_ISSUE)
        assertThat(impacts.single { it.action == EDIT_ISSUE }).satisfies { editIssueImpact ->
            assertThat(editIssueImpact.relative).isBetween(160.0, 165.0)
            assertThat(editIssueImpact.absolute).isBetween(Duration.ofMinutes(1), Duration.ofMinutes(2))
            assertThat(editIssueImpact.regression).isTrue()
            assertThat(editIssueImpact.signal).isTrue()
        }
        assertThat(verdict.reports).hasSize(2)
        assertThat(verdict.reports).allSatisfy { !it.successful }
        assertThat(verdict.reports.first()).satisfies { report ->
            assertThat(report.toXml("dummyTestCase"))
                .contains("There is a regression in [Full Edit Issue] with 95% confidence level. Regression is larger than allowed +10.00% tolerance")
        }
    }
}
