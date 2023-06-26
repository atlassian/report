package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionResult
import com.atlassian.performance.tools.report.api.result.DurationData
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import java.time.Duration
import java.time.Instant.now

class ActionMetricsReaderTest {

    private val reader = ActionMetricsReader()

    @Test
    fun shouldParseStandardDuration() {
        // given an external contract
        val metrics = JiraActionsContracts.output1 // dep on A

        // when
        val stats = reader.read(metrics)

        // then ?
        assertThat(stats).isEqualTo(ReportLibContracts.ActionMetricsReaderContracts.output1) // confirm contract B?
        val maxEditIssueTiming = stats["View Dashboard"]!!.stats.max
        val approximateMaxTiming = Duration.ofMillis(400)
        assertThat(maxEditIssueTiming).isCloseTo(approximateMaxTiming.toNanos().toDouble(), Offset.offset(1.0))
    }

    object ReportLibContracts {
        object ActionMetricsReaderContracts {
            val output1: Map<String, DurationData> = mapOf(
                "View Dashboard" to DurationData(
                    DescriptiveStatistics(doubleArrayOf(4.0E8)),
                    DurationData.createEmptyNanoseconds().durationMapping
                )
            )
        }
    }

    object JiraActionsContracts {
        val output1: List<ActionMetric> = listOf(
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
        )
    }
}
