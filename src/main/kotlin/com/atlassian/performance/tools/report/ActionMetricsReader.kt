package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.ActionMetric
import com.atlassian.performance.tools.jiraactions.ActionResult

class ActionMetricsReader {

    fun read(
        metrics: List<ActionMetric>
    ): Map<String, DurationData> = metrics
        .filter { it.result == ActionResult.OK }
        .groupBy { it.label }
        .mapValues { it.value.fold(DurationData.createEmptyNanoseconds(), this::addActionMetric) }

    private fun addActionMetric(
        durationData: DurationData,
        actionMetric: ActionMetric
    ): DurationData {
        durationData.stats.addValue(actionMetric.duration.toNanos().toDouble())
        return durationData
    }
}
