package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionResult
import com.atlassian.performance.tools.report.api.result.DurationData

internal class ActionMetricsReader {

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
