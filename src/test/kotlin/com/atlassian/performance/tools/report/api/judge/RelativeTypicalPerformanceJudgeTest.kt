package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.EDIT_ISSUE
import com.atlassian.performance.tools.jiraactions.api.VIEW_ISSUE
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.result.FakeResults
import com.atlassian.performance.tools.report.api.result.LocalRealResult
import com.atlassian.performance.tools.report.result.PerformanceStats
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths
import java.time.Duration
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds
import java.util.function.Consumer

class RelativeTypicalPerformanceJudgeTest {

    @Rule
    @JvmField
    val workspace = TemporaryFolder()

    @Test
    fun shouldJudgeWhenActionResultsAreMissingInBaseline() {
        // given
        val toleranceRatios = mapOf<ActionType<*>, Float>(VIEW_ISSUE to 1000F)
        val noCenters = mapOf<String, Duration>()
        val someCenters = mapOf<String, Duration>("View Issue" to ofMillis(1000))
        val baselineStats = PerformanceStats("baselineCohort", emptyMap(), noCenters, emptyMap(), emptyMap())
        val experimentStats = PerformanceStats("experimentCohort", emptyMap(), someCenters, emptyMap(), emptyMap())
        val impacts = mutableListOf<LatencyImpact>()
        val judge = RelativeTypicalPerformanceJudge.Builder()
            .handleLatencyImpact(Consumer { impacts.add(it) })
            .build()

        // when
        val verdict = judge.judge(toleranceRatios, baselineStats, experimentStats)

        // then
        with(SoftAssertions()) {
            assertThat(verdict.reports).hasSize(1)
            assertThat(verdict.reports.single().extractText())
                .contains("No action View Issue results for baselineCohort")
            assertThat(impacts).isEmpty()
            assertAll()
        }
    }

    @Test
    fun shouldJudgeWhenActionResultsAreMissingInExperiment() {
        // given
        val toleranceRatios = mapOf<ActionType<*>, Float>(VIEW_ISSUE to 1000F)
        val noCenters = mapOf<String, Duration>()
        val someCenters = mapOf<String, Duration>("View Issue" to ofMillis(1000))
        val baselineStats = PerformanceStats("baselineCohort", emptyMap(), someCenters, emptyMap(), emptyMap())
        val experimentStats = PerformanceStats("experimentCohort", emptyMap(), noCenters, emptyMap(), emptyMap())
        val impacts = mutableListOf<LatencyImpact>()
        val judge = RelativeTypicalPerformanceJudge.Builder()
            .handleLatencyImpact(Consumer { impacts.add(it) })
            .build()

        // when
        val verdict = judge.judge(toleranceRatios, baselineStats, experimentStats)

        // then
        assertThat(verdict.reports).hasSize(1)
        with(SoftAssertions()) {
            assertThat(verdict.reports.single().extractText())
                .contains("No action View Issue results for experimentCohort")
            assertThat(impacts).isEmpty()
            assertAll()
        }
    }

    @Test
    fun shouldJudgeNoise() {
        // given
        val toleranceRatios = mapOf<ActionType<*>, Float>(VIEW_ISSUE to 0.02f)
        val baselineStats = LocalRealResult(Paths.get("JIRA-JPT760-JOB1-8/alpha")).loadEdible().stats
        val experimentStats = LocalRealResult(Paths.get("JIRA-JPT760-JOB1-8/beta")).loadEdible().stats
        val impacts = mutableListOf<LatencyImpact>()
        val judge = RelativeTypicalPerformanceJudge.Builder()
            .handleLatencyImpact(Consumer { impacts.add(it) })
            .build()

        // when
        val verdict = judge.judge(toleranceRatios, baselineStats, experimentStats)

        // then
        assertThat(verdict.reports).hasSize(1)
        assertThat(impacts).hasSize(1)
        with(SoftAssertions()) {
            val impact = impacts.single()
            assertThat(impact.relevant).`as`("impact").isFalse()
            assertThat(impact.irrelevant).`as`("noise").isTrue()
            assertThat(impact.regression).`as`("regression").isFalse()
            assertThat(impact.relativeDiff).isBetween(0.013, 0.014)
            assertThat(impact.absoluteDiff).isBetween(ofMillis(15), ofMillis(16))
            assertAll()
        }
    }

    @Test
    fun shouldJudgeRegression() {
        // given
        val tolerances = FakeResults.actionTypes.associate { it to 0.02f }.toMap()
        val baseline = FakeResults.fastResult.stats
        val experiment = FakeResults.slowResult.stats
        val impacts = mutableListOf<LatencyImpact>()
        val judge = RelativeTypicalPerformanceJudge.Builder()
            .handleLatencyImpact(Consumer { impacts.add(it) })
            .build()

        // when
        val verdict = judge.judge(tolerances, baseline, experiment)

        // then
        val reports = verdict.reports
        assertThat(reports).hasSize(2)
        assertThat(impacts).hasSize(2)
        with(SoftAssertions()) {
            assertThat(reports[0].extractText())
                .contains("Full Edit Issue +16293% typical performance regression overcame +2% tolerance")
            assertThat(reports[1].extractText())
                .contains("Full Add Comment +16293% typical performance regression overcame +2% tolerance")
            val impact = impacts.first()
            assertThat(impact.action).isEqualTo(EDIT_ISSUE)
            assertThat(impact.relevant).`as`("impact").isTrue()
            assertThat(impact.irrelevant).`as`("noise").isFalse()
            assertThat(impact.regression).`as`("regression").isTrue()
            assertThat(impact.absoluteDiff).isBetween(ofSeconds(99), ofSeconds(101))
            assertThat(impact.relativeDiff).isBetween(160.0, 170.0)
            assertAll()
        }
    }


    @Test
    fun shouldJudgeImprovement() {
        // given
        val tolerances = FakeResults.actionTypes.associate { it to 0.02f }.toMap()
        val baseline = FakeResults.slowResult.stats
        val experiment = FakeResults.fastResult.stats
        val impacts = mutableListOf<LatencyImpact>()
        val judge = RelativeTypicalPerformanceJudge.Builder()
            .handleLatencyImpact(Consumer { impacts.add(it) })
            .build()

        // when
        val verdict = judge.judge(tolerances, baseline, experiment)

        // then
        assertThat(verdict.reports).hasSize(2)
        assertThat(impacts).hasSize(2)
        with(SoftAssertions()) {
            val impact = impacts.first()
            assertThat(impact.action).isEqualTo(EDIT_ISSUE)
            assertThat(impact.relevant).`as`("impact").isTrue()
            assertThat(impact.irrelevant).`as`("noise").isFalse()
            assertThat(impact.improvement).`as`("improvement").isTrue()
            assertThat(impact.regression).`as`("regression").isFalse()
            assertThat(impact.absoluteDiff).isBetween(ofSeconds(-101), ofSeconds(-99))
            assertThat(impact.relativeDiff).isBetween(-0.994, -0.993)
            assertAll()
        }
    }

    private fun JUnitReport.extractText() = toXml(javaClass.name)
}
