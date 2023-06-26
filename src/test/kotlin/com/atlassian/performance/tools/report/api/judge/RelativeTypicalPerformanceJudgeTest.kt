package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.VIEW_ISSUE
import com.atlassian.performance.tools.report.api.result.LocalRealResult
import com.atlassian.performance.tools.report.result.PerformanceStats
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths
import java.time.Duration
import java.time.Duration.*
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
        val thrown = catchThrowable {
            judge
                .judge(toleranceRatios, baselineStats, experimentStats)
                .assertAccepted(
                    "RelativeTypicalPerformanceJudgeTest",
                    workspace.newFolder().toPath(),
                    expectedReportCount = 1
                )
        }

        // then
        assertThat(thrown).hasMessageContaining(
            "Performance results are rejected, because:\nNo action View Issue results for baselineCohort"
        )
        assertThat(impacts).isEmpty()
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
        val thrown = catchThrowable {
            judge
                .judge(toleranceRatios, baselineStats, experimentStats)
                .assertAccepted(
                    "RelativeTypicalPerformanceJudgeTest",
                    workspace.newFolder().toPath(),
                    expectedReportCount = 1
                )
        }

        // then
        assertThat(thrown).hasMessageContaining(
            "Performance results are rejected, because:\nNo action View Issue results for experimentCohort"
        )
        assertThat(impacts).isEmpty()
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
        val thrown = catchThrowable {
            judge
                .judge(toleranceRatios, baselineStats, experimentStats)
                .assertAccepted(javaClass.name, workspace.newFolder().toPath(), expectedReportCount = 1)
        }

        // then
        assertThat(thrown).doesNotThrowAnyException()
        assertThat(impacts).hasSize(1)
        assertThat(impacts.single()).satisfies {
            assertThat(it.signal).isFalse()
            assertThat(it.noise).isTrue()
            assertThat(it.regression).isFalse()
            assertThat(it.relative).isBetween(0.013, 0.014)
            assertThat(it.absolute).isBetween(ofMillis(15), ofMillis(16))
        }
    }
}
