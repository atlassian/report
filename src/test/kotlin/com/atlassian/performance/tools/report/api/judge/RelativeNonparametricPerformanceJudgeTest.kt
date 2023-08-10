package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.EDIT_ISSUE
import com.atlassian.performance.tools.report.api.LatencyImpactMarkdownTable as DeprecatedLatencyImpactMarkdownTable
import com.atlassian.performance.tools.report.api.impact.LatencyImpactClassifier
import com.atlassian.performance.tools.report.api.impact.LatencyImpactMarkdownTable
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.result.FakeResults
import com.atlassian.performance.tools.report.api.result.FakeResults.addNoise
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import java.nio.file.Files.createTempDirectory
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
            experimentResult = FakeResults.fastResult.addNoise()
        )

        // then
        assertThat(verdict.reports).hasSize(2)
        assertThat(verdict.reports).allMatch { it.successful }
        assertThat(impacts).isNotEmpty()
        assertThat(impacts.map { it.action }).contains(EDIT_ISSUE)
        with(SoftAssertions()) {
            val impact = impacts.single { it.action == EDIT_ISSUE }
            assertThat(impact.relativeDiff).isBetween(-0.1, 0.1)
            assertThat(impact.absoluteDiff).isEqualTo(ofMillis(3))
            assertThat(impact.regression).isFalse()
            assertThat(impact.improvement).isFalse()
            assertThat(impact.irrelevant).isTrue()
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
        assertThat(impacts).isNotEmpty()
        with(SoftAssertions()) {
            assertThat(verdict.reports).allMatch { !it.successful }
            assertThat(verdict.reports.first().extractText())
                .contains("There is a regression in [Full Edit Issue] with 95% confidence level. Regression is larger than allowed +10.00% tolerance")
            assertThat(impacts.map { it.action }).contains(EDIT_ISSUE)
            val impact = impacts.single { it.action == EDIT_ISSUE }
            assertThat(impact.relativeDiff).isBetween(160.0, 165.0)
            assertThat(impact.absoluteDiff).isBetween(ofMinutes(1), ofMinutes(2))
            assertThat(impact.regression).`as`("regression").isTrue()
            assertThat(impact.relevant).`as`("impact").isTrue()
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
            assertThat(impact.relativeDiff).isBetween(-0.994, -0.993)
            assertThat(impact.absoluteDiff).isBetween(ofMinutes(-2), ofMinutes(-1))
            assertThat(impact.improvement).`as`("improvement").isTrue()
            assertThat(impact.relevant).`as`("impact").isTrue()
            assertAll()
        }
    }

    @Test
    fun shouldIntegrateWithDeprecatedTable() {
        // given
        val zeroToleranceRatios = FakeResults.actionTypes.associate { it to 0.1f }.toMap()
        val workspace = TestWorkspace(createTempDirectory(javaClass.simpleName))
        @Suppress("DEPRECATION") val judge = RelativeNonparametricPerformanceJudge.Builder()
            .handleLatencyImpact(DeprecatedLatencyImpactMarkdownTable(workspace))
            .build()

        // when
        judge.judge(
            toleranceRatios = zeroToleranceRatios,
            baselineResult = FakeResults.fastResult,
            experimentResult = FakeResults.slowResult
        )

        // then
        assertThat(workspace.directory.resolve("latency-impact-table.md")).hasContent(
            """
            | Action                | Classification | Confidence | Latency impact | Latency impact |
            |-----------------------|----------------|------------|----------------|----------------|
            | Full Edit Issue       | REGRESSION     | 68.27 %    | +16293 %       | +99390 ms      |
            | Full Add Comment      | REGRESSION     | 68.27 %    | +16293 %       | +99390 ms      |
            """.trimIndent()
        )
    }

    @Test
    fun shouldIntegrateWithTable() {
        // given
        val zeroToleranceRatios = FakeResults.actionTypes.associate { it to 0.1f }.toMap()
        val workspace = TestWorkspace(createTempDirectory(javaClass.simpleName))
        val classifier = LatencyImpactClassifier.Builder()
            .handleLatencyImpact(LatencyImpactMarkdownTable(workspace))
            .build()
        val judge = RelativeNonparametricPerformanceJudge.Builder()
            .handleLatencyImpact(classifier)
            .build()

        // when
        judge.judge(
            toleranceRatios = zeroToleranceRatios,
            baselineResult = FakeResults.fastResult,
            experimentResult = FakeResults.slowResult
        )

        // then
        assertThat(workspace.directory.resolve("latency-impact-table.md")).hasContent(
            """
            | Action                | Classification | Confidence | Latency impact | Latency impact |
            |-----------------------|----------------|------------|----------------|----------------|
            | Full Edit Issue       | REGRESSION     | 68.27 %    | +16293 %       | +99390 ms      |
            | Full Add Comment      | REGRESSION     | 68.27 %    | +16293 %       | +99390 ms      |
            """.trimIndent()
        )
    }

    private fun JUnitReport.extractText() = toXml(javaClass.name)
}
