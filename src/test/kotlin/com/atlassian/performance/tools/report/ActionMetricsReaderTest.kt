package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionResult
import org.hamcrest.Matchers.closeTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant.now
import java.util.*

class ActionMetricsReaderTest {

    private val reader = ActionMetricsReader()

    @Test
    fun shouldParseStandardDuration() {
        val stats = reader.read(listOf(
            ActionMetric.Builder(
                label = "View Dashboard",
                result = ActionResult.OK,
                duration = Duration.ofMillis(400),
                start = now()
            ).build(),
            ActionMetric.Builder(
                label = "View Dashboard",
                result = ActionResult.ERROR,
                duration = Duration.ofMillis(2000),
                start = now()
            ).build(),
            ActionMetric.Builder(
                label = "View Dashboard",
                result = ActionResult.OK,
                duration = Duration.ofMillis(100),
                start = now()
            ).build()
        ))

        val maxEditIssueTiming = stats["View Dashboard"]!!.stats.max
        val approximateMaxTiming = Duration.ofMillis(400)
        val precision = Duration.ofMillis(1)
        assertThat(
            maxEditIssueTiming,
            closeTo(
                approximateMaxTiming.toNanos().toDouble(),
                precision.toNanos().toDouble()
            )
        )
    }
}
