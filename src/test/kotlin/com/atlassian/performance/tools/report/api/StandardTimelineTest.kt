package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionResult
import com.atlassian.performance.tools.jiraactions.api.BROWSE_BOARDS
import com.atlassian.performance.tools.report.api.result.LocalRealResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

class StandardTimelineTest {
    @Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    @Test
    fun shouldCropColdCache() {
        val metrics = loadMetrics()

        val cropped = StandardTimeline(Duration.ofMinutes(20)).crop(metrics)

        val actualEarliest = cropped
            .filter { it.label == BROWSE_BOARDS.label }
            .minBy { it.start }!!
        val earlyAndCold = ActionMetric.Builder(
            label = BROWSE_BOARDS.label,
            start = Instant.parse("2018-04-27T09:22:00.537Z"),
            result = ActionResult.OK,
            duration = Duration.ofMillis(15359)
        ).build()
        assertThat(actualEarliest).isNotEqualTo(earlyAndCold)
    }

    private fun loadMetrics(): List<ActionMetric> {
        return LocalRealResult(Paths.get("JIRA-JPT-9107"))
            .loadRaw(tempFolder)
            .prepareForJudgement(FullTimeline()).actionMetrics
    }

    @Test
    fun shouldInformAboutOvercropping() {
        val shortMetrics = loadMetrics().take(2)

        val thrown = catchThrowable {
            StandardTimeline(Duration.ofSeconds(10)).crop(shortMetrics)
        }

        assertThat(thrown)
            .hasMessageContaining("action metrics contained only cold-cache results")
            .hasMessageContaining("increase the load duration")
            .hasMessageContaining("stop cold-cache cropping")
    }
}
