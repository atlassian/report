package com.atlassian.performance.tools.report.distribution

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.report.JsonStyle
import com.atlassian.performance.tools.report.Point
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.chart.Chart
import com.atlassian.performance.tools.report.chart.ChartLine
import com.atlassian.performance.tools.workspace.api.git.GitRepo
import org.apache.logging.log4j.LogManager
import java.nio.file.Path

internal class DistributionComparison(
    private val repo: GitRepo
) {

    private val logger = LogManager.getLogger(this::class.java)

    fun compare(
        results: List<EdibleResult>,
        output: Path
    ) {
        val quantileFunction = summarize(results, "latency-axis") { QuantileFunction().plot(it) }
        val report = this::class
            .java
            .getResourceAsStream("distribution-comparison-template.html")
            .bufferedReader()
            .use { it.readText() }
            .replace(
                oldValue = "'<%= quantileFunction =%>'",
                newValue = print(quantileFunction)
            )
            .replace(
                oldValue = "'<%= commit =%>'",
                newValue = repo.getHead()
            )
        output.toFile().ensureParentDirectory().printWriter().use { it.print(report) }
        logger.info("Distribution comparison available at ${output.toUri()}")
    }

    private fun <T : Comparable<T>> summarize(
        results: List<EdibleResult>,
        yAxisId: String,
        summarize: (metrics: List<Int>) -> List<Point<T>>
    ): Chart<T> = Chart(
        results.flatMap { result ->
            summarizeEntireCohort(summarize, result, yAxisId) + summarizeEachActionType(result, summarize, yAxisId)
        }
    )

    private fun <T : Comparable<T>> summarizeEntireCohort(
        summarize: (metrics: List<Int>) -> List<Point<T>>,
        result: EdibleResult,
        yAxisId: String
    ): List<ChartLine<T>> = listOf(
        ChartLine(
            data = summarize(result.actionMetrics.map { it.duration.toMillis().toInt() }),
            label = result.cohort,
            cohort = result.cohort,
            type = "line",
            yAxisId = yAxisId,
            hidden = false
        )
    )

    private fun <T : Comparable<T>> summarizeEachActionType(
        result: EdibleResult,
        summarize: (metrics: List<Int>) -> List<Point<T>>,
        yAxisId: String
    ): List<ChartLine<T>> = result
        .actionMetrics
        .groupBy { it.label }
        .map { (actionType, metrics) ->
            ChartLine(
                data = summarize(metrics.map { it.duration.toMillis().toInt() }),
                label = "${result.cohort}: $actionType",
                cohort = result.cohort,
                type = "line",
                yAxisId = yAxisId,
                hidden = true
            )
        }

    private fun print(chart: Chart<*>): String = JsonStyle().prettyPrint(chart.toJson())
}
