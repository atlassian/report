package com.atlassian.performance.tools.report.result

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionResult
import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.OutlierTrimming
import com.atlassian.performance.tools.report.api.result.EdibleResult
import org.apache.commons.math3.stat.descriptive.rank.Max
import org.apache.commons.math3.stat.descriptive.rank.Min
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant

class InternalStatsMeterTest {

    @Test
    fun shouldMeasure() {
        val statsMeter = InternalStatsMeter()
        val action1 = "action1"
        val action2 = "action2"
        val edibleResult = EdibleResult.Builder("test")
            .actionMetrics(
                listOf(
                    createActionMetric(action1, Duration.ofSeconds(1)),
                    createActionMetric(action1, Duration.ofSeconds(2)),
                    createActionMetric(action1, Duration.ofSeconds(3)),
                    createActionMetric(action1, Duration.ofSeconds(100), ActionResult.ERROR),
                    createActionMetric(action2, Duration.ofSeconds(4)),
                    createActionMetric(action2, Duration.ofSeconds(1)),
                    createActionMetric(action2, Duration.ofSeconds(6))
                )
            )
            .build()

        val stats = statsMeter.measurePerformance(
            result = edibleResult,
            centralTendencyMetric = Max(),
            dispersionMetric = Min(),
            trimmingPerType = mapOf(
                ActionType<Any>(action1) {Unit} to OutlierTrimming(lowerTrim = 0.0, upperTrim = 1.0),
                ActionType<Any>(action2) {Unit} to OutlierTrimming(lowerTrim = 0.0, upperTrim = 1.0)
            )
        )

        val sampleSizes = stats.sampleSizes
        val dispersions = stats.dispersions
        val errors = stats.errors

        assertThat(sampleSizes!![action1]).isEqualTo(3)
        assertThat(sampleSizes[action2]).isEqualTo(3)
        assertThat(errors!![action1]).isEqualTo(1)
        assertThat(errors[action2]).isEqualTo(0)
        assertThat(dispersions).containsEntry(action1, Duration.ofSeconds(1))
        assertThat(dispersions).containsEntry(action2, Duration.ofSeconds(1))
    }

    @Test
    fun measureAccordingToProvidedOutliers() {
        val searchWithJql = "Search with JQL"
        val searchWithJqlType = ActionType<Any>(searchWithJql) { Unit }
        val result = EdibleResult.Builder("test")
            .actionMetrics(
                listOf(
                    createActionMetric(searchWithJql, Duration.ofSeconds(1)),
                    createActionMetric(searchWithJql, Duration.ofSeconds(10))
                ))
            .trimmingPerType(
                mapOf(searchWithJqlType to OutlierTrimming(lowerTrim = 0.0, upperTrim = 0.5)))
            .build()

        assertThat(result.stats.locations[searchWithJql]).isEqualTo(Duration.parse("PT1.0S"))
    }

    private fun createActionMetric(label: String, duration: Duration, result: ActionResult = ActionResult.OK): ActionMetric {
        return ActionMetric.Builder(
            label = label,
            result = result,
            start = Instant.now(),
            duration = duration
        ).build()
    }
}
