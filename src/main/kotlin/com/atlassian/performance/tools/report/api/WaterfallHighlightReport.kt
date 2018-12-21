package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.report.chart.waterfall.WaterfallChart
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import java.nio.file.Path

/**
 * Produces two waterfall charts for each supplied [ActionMetric]:
 * typical-waterfall - for action with median duration
 * pessimistic-waterfall - for action with 99th percentile duration
 *
 * @see com.atlassian.performance.tools.jiraactions.api.ActionMetric
 */
class WaterfallHighlightReport {

    fun report(
        metrics: List<ActionMetric>,
        workspace: TestWorkspace
    ) {
        val reportDirectory = workspace.directory.resolve("waterfalls")
        metrics
            .groupBy { it.label }
            .mapValues { summarize(it.value) }
            .forEach { waterfall(it.key, it.value, reportDirectory) }
    }

    private fun summarize(
        metrics: List<ActionMetric>
    ): ActionTypeSummary {
        val sortedMetrics = metrics.sortedBy { it.duration }
        return ActionTypeSummary(
            typicalLatency = getPercentile(sortedMetrics, 0.5),
            pessimisticLatency = getPercentile(sortedMetrics, 0.99)
        )
    }

    private fun getPercentile(
        metrics: List<ActionMetric>,
        quantile: Double
    ) = metrics[(metrics.size.toDouble() * quantile).toInt()]

    private fun waterfall(
        label: String,
        summary: ActionTypeSummary,
        reports: Path
    ) {
        val waterfall = WaterfallChart()
        waterfall.plot(
            summary.typicalLatency,
            reports.resolve(label).resolve("typical-waterfall.html").toFile()
        )

        waterfall.plot(
            summary.pessimisticLatency,
            reports.resolve(label).resolve("pessimistic-waterfall.html").toFile()
        )
    }

    private class ActionTypeSummary(
        val typicalLatency: ActionMetric,
        val pessimisticLatency: ActionMetric
    )
}