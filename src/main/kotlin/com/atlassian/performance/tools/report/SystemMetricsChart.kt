package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.infrastructure.metric.Dimension
import com.atlassian.performance.tools.infrastructure.metric.SystemMetric
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.json.Json
import javax.json.JsonObject

class SystemMetricsChart(
    private val title: String,
    private val allMetrics: List<SystemMetric>,
    private val dimension: Dimension,
    private val axis: ChartAxis
) {
    constructor(
        allMetrics: List<SystemMetric>,
        dimension: Dimension,
        axisId: String
    ) : this(
        title = dimension.description,
        allMetrics = allMetrics,
        dimension = dimension,
        axis = ChartAxis(
            id = axisId,
            text = dimension.description
        )
    )

    fun toJson(): JsonObject {
        val metrics = allMetrics
            .filter { it.dimension == dimension }
            .sortedBy { it.start }
        val chartData = Chart(plotValuesPerSystem(metrics))
        return Json.createObjectBuilder()
            .add("title", title)
            .add("axis", axis.toJson())
            .add("data", chartData.toJson())
            .build()
    }

    private fun plotValuesPerSystem(
        metrics: List<SystemMetric>
    ): List<ChartLine<Instant>> {
        return metrics
            .map { it.system }.toSet().sorted()
            .map { system ->
                getReducedValue(
                    metrics = metrics.filter { it.system == system },
                    label = system
                )
            }
    }

    private fun getReducedValue(
        metrics: List<SystemMetric>,
        label: String
    ): ChartLine<Instant> {
        return metrics
            .toChartLine(
                label = label,
                type = "line",
                yAxisId = axis.id,
                hidden = false
            ) { dimension.reduction.lambda(it.map { it.value }) }
    }

    private fun List<SystemMetric>.toChartLine(
        label: String,
        yAxisId: String,
        type: String,
        hidden: Boolean,
        reduce: (List<SystemMetric>) -> Double
    ): ChartLine<Instant> {
        val data = this
            .asSequence()
            .groupBy { it.start.truncatedTo(ChronoUnit.MINUTES) }
            .mapValues { entry ->
                reduce(entry.value)
            }
            .map {
                Tick(
                    time = it.key,
                    value = it.value
                )
            }

        return ChartLine(
            label = label,
            type = type,
            yAxisId = yAxisId,
            data = data,
            hidden = hidden
        )
    }
}