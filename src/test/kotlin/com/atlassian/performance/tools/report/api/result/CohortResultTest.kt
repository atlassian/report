package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.infrastructure.virtualusers.GrowingLoadSchedule
import com.atlassian.performance.tools.infrastructure.virtualusers.LoadProfile
import com.atlassian.performance.tools.jiraactions.ActionMetric
import com.atlassian.performance.tools.jiraactions.ActionResult
import com.atlassian.performance.tools.jiraactions.BROWSE_BOARDS
import com.atlassian.performance.tools.jiraactions.SEARCH_WITH_JQL
import com.atlassian.performance.tools.report.api.ColdCachesTimeline
import com.atlassian.performance.tools.report.api.TestExecutionTimeline
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Assert.assertThat
import org.junit.Test
import java.nio.file.Paths
import java.time.Duration.ofMillis
import java.time.Duration.ofMinutes
import java.time.Instant
import java.util.*

class CohortResultTest {

    @Test
    fun shouldCropColdCache() {
        val result = LocalRealResult(Paths.get("JIRA-JPT-9107")).loadRaw()

        val metrics = result.prepareForJudgement(
            ColdCachesTimeline()
        ).actionMetrics

        val actualEarliest = metrics
            .filter { it.label == BROWSE_BOARDS.label }
            .sortedBy { it.start }
            .first()
        val earlyAndCold = ActionMetric(
            label = BROWSE_BOARDS.label,
            start = Instant.parse("2018-04-27T09:22:00.537Z"),
            result = ActionResult.OK,
            duration = ofMillis(15359),
            virtualUser = UUID.fromString("3db7be47-daed-4929-980c-4668e61136b8"),
            observation = null
        )
        assertThat(actualEarliest, not(equalTo(earlyAndCold)))
    }

    @Test
    fun shouldCropStragglers() {
        val result = LocalRealResult(Paths.get("JIRA-JPTS1-23")).loadRaw()
        val loadProfile = LoadProfile(
            loadSchedule = GrowingLoadSchedule(
                duration = ofMinutes(20), // in reality this was PT32M, but the test explores "what if?"
                initialNodes = 1,
                finalNodes = 28
            ),
            virtualUsersPerNode = 20,
            seed = 1234
        )

        val metrics = result.prepareForJudgement(
            TestExecutionTimeline(loadProfile.loadSchedule.duration)
        ).actionMetrics

        val actualLatest = metrics
            .sortedByDescending { it.start }
            .first()
        val straggler = ActionMetric(
            label = SEARCH_WITH_JQL.label,
            start = Instant.parse("2018-04-27T15:12:27.740Z"),
            result = ActionResult.ERROR,
            duration = ofMinutes(5) + ofMillis(10),
            virtualUser = UUID.fromString("0844c81e-c860-4b75-abde-ef69a5167a3f"),
            observation = null
        )
        assertThat(actualLatest, not(equalTo(straggler)))
    }
}
