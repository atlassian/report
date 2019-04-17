package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.VIEW_ISSUE
import com.atlassian.performance.tools.report.result.PerformanceStats
import org.assertj.core.api.Assertions.catchThrowable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Duration

class RelativeTypicalPerformanceJudgeTest {

    @Rule
    @JvmField
    val workspace = TemporaryFolder()

    @Test
    fun shouldJudgeWhenActionResultsAreMissingInBaseline() {
        // given
        val toleranceRatios = mapOf<ActionType<*>, Float>(VIEW_ISSUE to 1000F)
        val noCenters = mapOf<String, Duration>()
        val someCenters = mapOf<String, Duration>("View Issue" to Duration.ofMillis(1000))
        val baselineStats = PerformanceStats("baselineCohort", emptyMap(), noCenters, emptyMap(), emptyMap())
        val experimentStats = PerformanceStats("experimentCohort", emptyMap(), someCenters, emptyMap(), emptyMap())

        // when
        val thrown = catchThrowable {
            RelativeTypicalPerformanceJudge()
                    .judge(toleranceRatios, baselineStats, experimentStats)
                    .assertAccepted("RelativeTypicalPerformanceJudgeTest", workspace.newFolder().toPath(), expectedReportCount = 1)
        }

        // then
        assertThat(thrown).hasMessageContaining("Performance results are rejected, because:\n" +
                "No action View Issue results for baselineCohort")
    }

    @Test
    fun shouldJudgeWhenActionResultsAreMissingInExperiment() {
        // given
        val toleranceRatios = mapOf<ActionType<*>, Float>(VIEW_ISSUE to 1000F)
        val noCenters = mapOf<String, Duration>()
        val someCenters = mapOf<String, Duration>("View Issue" to Duration.ofMillis(1000))
        val baselineStats = PerformanceStats("baselineCohort", emptyMap(), someCenters, emptyMap(), emptyMap())
        val experimentStats = PerformanceStats("experimentCohort", emptyMap(), noCenters, emptyMap(), emptyMap())

        // when
        val thrown = catchThrowable {
            RelativeTypicalPerformanceJudge()
                    .judge(toleranceRatios, baselineStats, experimentStats)
                    .assertAccepted("RelativeTypicalPerformanceJudgeTest", workspace.newFolder().toPath(), expectedReportCount = 1)
        }

        // then
        assertThat(thrown).hasMessageContaining("Performance results are rejected, because:\n" +
                "No action View Issue results for experimentCohort")
    }
}