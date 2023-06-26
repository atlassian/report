package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionResult
import com.atlassian.performance.tools.report.api.result.DurationData

internal class ActionMetricsReader {

    fun read(
        metrics: List<ActionMetric>
    ): Map<String, DurationData> {
        return metrics
            .filter { it.result == ActionResult.OK }
            .groupBy { it.label }
            .mapValues { it.value.fold(createEmptyData(), this::addActionMetric) }
    }

    private fun createEmptyData() = DurationData.createEmptyNanoseconds()

    fun convertToDuration(double: Double) = createEmptyData().durationMapping(double)

    private fun addActionMetric(
        durationData: DurationData,
        actionMetric: ActionMetric
    ): DurationData {
        val double = actionMetric.duration.toNanos().toDouble()
        durationData.stats.addValue(double)
        return durationData
    }
}
