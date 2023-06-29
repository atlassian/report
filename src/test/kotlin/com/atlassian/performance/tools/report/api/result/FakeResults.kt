package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.jiraactions.api.*
import java.time.Duration
import java.time.Instant

object FakeResults {
    val actionTypes: List<ActionType<*>> = listOf(EDIT_ISSUE, ADD_COMMENT)

    val fastResult = EdibleResult.Builder("fast")
        .actionMetrics(metrics { Duration.ofMillis(10).plusMillis(it * 10) })
        .build()

    val slowResult = EdibleResult.Builder("slow")
        .actionMetrics(metrics { Duration.ofSeconds(100) })
        .build()

    private fun metrics(
        latencyGenerator: (index: Long) -> Duration
    ): List<ActionMetric> = LongRange(0, 120).map { index ->
        actionTypes.map { actionType ->
            ActionMetric.Builder(
                actionType.label,
                ActionResult.OK,
                latencyGenerator(index),
                Instant.now().minusSeconds(index)
            ).build()
        }
    }.flatten()

    fun EdibleResult.addNoise() = EdibleResult.Builder(this.cohort)
        .actionMetrics(this.actionMetrics.addNoise())
        .build()

    private fun List<ActionMetric>.addNoise(): List<ActionMetric> = this.mapIndexed { index, metric ->
        ActionMetric.Builder(
            metric.label,
            metric.result,
            metric.duration.addNoise(index),
            metric.start
        ).build()
    }

    private fun Duration.addNoise(index: Int): Duration {
        val noise = Duration.ofMillis((index * 11L) % 7)
        return if (index % 2 == 0) {
            this.plus(noise)
        } else {
            this.minus(noise)
        }
    }
}
