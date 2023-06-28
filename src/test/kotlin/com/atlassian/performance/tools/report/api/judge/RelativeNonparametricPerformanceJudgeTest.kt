package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.EDIT_ISSUE
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.result.FakeResults
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import java.time.Duration.ofMillis
import java.time.Duration.ofMinutes
import java.util.function.Consumer

class RelativeNonparametricPerformanceJudgeTest {

    @Test
    fun shouldJudgeNoise() {
        // given
        val zeroToleranceRatios = FakeResults.actionTypes.associate { it to 0.1f }.toMap()
        val impacts = mutableListOf<LatencyImpact>()
        val judge = RelativeNonparametricPerformanceJudge.Builder()
            .handleLatencyImpact(Consumer { impacts.add(it) })
            .build()

        // when
        val verdict = judge.judge(
            toleranceRatios = zeroToleranceRatios,
            baselineResult = FakeResults.fastResult,
            experimentResult = FakeResults.fastResult
        )

        // then
        assertThat(verdict.reports).hasSize(2)
        assertThat(verdict.reports).allMatch { it.successful }
        assertThat(impacts).isNotEmpty()
        assertThat(impacts.map { it.action }).contains(EDIT_ISSUE)
        assertThat(impacts.single { it.action == EDIT_ISSUE }).satisfies { editIssueImpact ->
            assertThat(editIssueImpact.relative).isBetween(-0.1, 0.1)
            assertThat(editIssueImpact.absolute).isBetween(ofMillis(-1), ofMillis(1))
            assertThat(editIssueImpact.regression).isFalse()
            assertThat(editIssueImpact.improvement).isFalse()
            assertThat(editIssueImpact.noise).isTrue()
        }
    }

    @Test
    fun shouldJudgeRegression() {
        // given
        val zeroToleranceRatios = FakeResults.actionTypes.associate { it to 0.1f }.toMap()
        val impacts = mutableListOf<LatencyImpact>()
        val judge = RelativeNonparametricPerformanceJudge.Builder()
            .handleLatencyImpact(Consumer { impacts.add(it) })
            .build()

        // when
        val verdict = judge.judge(
            toleranceRatios = zeroToleranceRatios,
            baselineResult = FakeResults.fastResult,
            experimentResult = FakeResults.slowResult
        )

        // then
        assertThat(verdict.reports).hasSize(2)
        with(SoftAssertions()) {
            assertThat(verdict.reports).allMatch { !it.successful }
            assertThat(verdict.reports.first().extractText())
                .contains("There is a regression in [Full Edit Issue] with 95% confidence level. Regression is larger than allowed +10.00% tolerance")
            assertThat(impacts).isNotEmpty()
            assertThat(impacts.map { it.action }).contains(EDIT_ISSUE)
            val impact = impacts.single { it.action == EDIT_ISSUE }
            assertThat(impact.relative).isBetween(160.0, 165.0)
            assertThat(impact.absolute).isBetween(ofMinutes(1), ofMinutes(2))
            assertThat(impact.regression).`as`("regression").isTrue()
            assertThat(impact.signal).`as`("signal").isTrue()
            assertAll()
        }
    }

    @Test
    fun shouldJudgeImprovement() {
        // given
        val zeroToleranceRatios = FakeResults.actionTypes.associate { it to 0.1f }.toMap()
        val impacts = mutableListOf<LatencyImpact>()
        val judge = RelativeNonparametricPerformanceJudge.Builder()
            .handleLatencyImpact(Consumer { impacts.add(it) })
            .build()

        // when
        val verdict = judge.judge(
            toleranceRatios = zeroToleranceRatios,
            baselineResult = FakeResults.slowResult,
            experimentResult = FakeResults.fastResult
        )

        // then
        assertThat(verdict.reports).hasSize(2)
        assertThat(impacts).isNotEmpty()
        with(SoftAssertions()) {
            assertThat(verdict.reports).allMatch { it.successful }
            assertThat(impacts.map { it.action }).contains(EDIT_ISSUE)
            val impact = impacts.single { it.action == EDIT_ISSUE }
            assertThat(impact.relative).isBetween(-0.994, -0.993)
            assertThat(impact.absolute).isBetween(ofMinutes(-2), ofMinutes(-1))
            assertThat(impact.improvement).`as`("improvement").isTrue()
            assertThat(impact.signal).`as`("signal").isTrue()
            assertAll()
        }
    }

    private fun JUnitReport.extractText() = toXml(javaClass.name)
}
