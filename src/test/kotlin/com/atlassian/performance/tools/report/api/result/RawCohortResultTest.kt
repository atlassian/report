package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionResult
import com.atlassian.performance.tools.jiraactions.api.BROWSE_BOARDS
import com.atlassian.performance.tools.jiraactions.api.SEARCH_WITH_JQL
import com.atlassian.performance.tools.report.api.ColdCachesTimeline
import com.atlassian.performance.tools.report.api.TestExecutionTimeline
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths
import java.time.Duration.ofMillis
import java.time.Duration.ofMinutes
import java.time.Instant

class RawCohortResultTest {
    @Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    @Test
    fun shouldCropColdCache() {
        val result = LocalRealResult(Paths.get("JIRA-JPT-9107")).loadRaw(tempFolder)

        val metrics = result.prepareForJudgement(
            ColdCachesTimeline()
        ).actionMetrics

        val actualEarliest = metrics
            .filter { it.label == BROWSE_BOARDS.label }
            .sortedBy { it.start }
            .first()
        val earlyAndCold = ActionMetric.Builder(
            label = BROWSE_BOARDS.label,
            start = Instant.parse("2018-04-27T09:22:00.537Z"),
            result = ActionResult.OK,
            duration = ofMillis(15359)
        ).build()
        assertThat(actualEarliest).isNotEqualTo(earlyAndCold)
    }

    @Test
    fun shouldCropStragglers() {
        val result = LocalRealResult(Paths.get("JIRA-JPTS1-23")).loadRaw(tempFolder)

        val metrics = result.prepareForJudgement(
            TestExecutionTimeline(ofMinutes(20))
        ).actionMetrics

        val actualLatest = metrics
            .sortedByDescending { it.start }
            .first()
        val straggler = ActionMetric.Builder(
            label = SEARCH_WITH_JQL.label,
            start = Instant.parse("2018-04-27T15:12:27.740Z"),
            result = ActionResult.ERROR,
            duration = ofMinutes(5) + ofMillis(10)
        ).build()
        assertThat(actualLatest).isNotEqualTo(straggler)
    }
}
