package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.infrastructure.metric.Dimension
import com.atlassian.performance.tools.infrastructure.metric.SystemMetric
import com.atlassian.performance.tools.io.ensureParentDirectory
import com.atlassian.performance.tools.jiraactions.ActionMetric
import com.atlassian.performance.tools.workspace.api.git.GitRepo
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import javax.json.Json
import javax.json.JsonArray

internal class TimelineChart(
    private val repo: GitRepo
) {
    private val logger = LogManager.getLogger(this::class.java)

    fun generate(
        output: Path,
        actionMetrics: List<ActionMetric>,
        systemMetrics: List<SystemMetric>
    ) {
        val trimmedSystemMetrics = trimSystemMetrics(actionMetrics, systemMetrics)
        val report = this::class
            .java
            .getResourceAsStream("timeline-chart-template.html")
            .bufferedReader()
            .use { it.readText() }
            .replace(
                oldValue = "'<%= virtualUserChartData =%>'",
                newValue = ChartBuilder().build(actionMetrics).toJson().toString()
            )
            .replace(
                oldValue = "'<%= systemMetricsCharts =%>'",
                newValue = systemMetricsCharts(trimmedSystemMetrics).toString()
            )
            .replace(
                oldValue = "'<%= commit =%>'",
                newValue = repo.getHead()
            )
        output.toFile().ensureParentDirectory().printWriter().use { it.print(report) }
        logger.info("Timeline chart available at ${output.toUri()}")
    }

    private fun systemMetricsCharts(
        trimmedSystemMetrics: List<SystemMetric>
    ): JsonArray {
        return Json
            .createArrayBuilder()
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.CPU_LOAD,
                axisId = "cpu-load-axis"
            ).toJson())
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.JSTAT_SURVI_0,
                axisId = "survi-0-axis"
            ).toJson())
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.JSTAT_SURVI_1,
                axisId = "survi-1-axis"
            ).toJson())
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.JSTAT_EDEN,
                axisId = "eden-axis"
            ).toJson())
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.JSTAT_OLD,
                axisId = "old-axis"
            ).toJson())
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.JSTAT_COMPRESSED_CLASS,
                axisId = "meta-axis"
            ).toJson())
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.JSTAT_YOUNG_GEN_GC,
                axisId = "young-gc-axis"
            ).toJson())
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.JSTAT_YOUNG_GEN_GC_TIME,
                axisId = "young-gc-time-axis"
            ).toJson())
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.JSTAT_FULL_GC,
                axisId = "full-gc-axis"
            ).toJson())
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.JSTAT_FULL_GC_TIME,
                axisId = "full-gc-time-axis"
            ).toJson())
            .add(SystemMetricsChart(
                allMetrics = trimmedSystemMetrics,
                dimension = Dimension.JSTAT_TOTAL_GC_TIME,
                axisId = "total-gc-time-axis"
            ).toJson())
            .build()
    }

    private fun trimSystemMetrics(
        actionMetrics: List<ActionMetric>,
        systemMetrics: List<SystemMetric>
    ): List<SystemMetric> {
        if (actionMetrics.isEmpty()) {
            return emptyList()
        }

        val metricsSortedByTime = actionMetrics
            .sortedBy { it.start }

        val beginning = metricsSortedByTime
            .first()
            .start

        val end = metricsSortedByTime
            .last()
            .start

        return systemMetrics
            .filter { it.start.isAfter(beginning) }
            .filter { it.start.isBefore(end) }
    }
}