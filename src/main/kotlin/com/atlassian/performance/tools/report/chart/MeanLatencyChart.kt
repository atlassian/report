package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.report.JsonProviderSingleton.JSON
import com.atlassian.performance.tools.report.JsonStyle
import com.atlassian.performance.tools.report.MeanAggregator
import com.atlassian.performance.tools.report.api.result.Stats
import org.apache.logging.log4j.LogManager
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ofLocalizedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.temporal.ChronoField.NANO_OF_SECOND
import javax.json.JsonArray
import javax.json.JsonObject

/**
 * Charts latencies per cohort.
 */
internal class MeanLatencyChart {
    private val logger = LogManager.getLogger(this::class.java)

    fun plot(
        stats: Collection<Stats>,
        labels: List<String>,
        output: File
    ) {
        val latencies = stats.map { CohortMeanLatency(it.cohort, MeanAggregator().aggregateCenters(labels, it)) }
        val report = this::class
            .java
            .getResourceAsStream("aggregate-chart-template.html")
            .bufferedReader()
            .use { it.readText() }
            .replace(
                oldValue = "'<%= aggregateChartData =%>'",
                newValue = prettyPrint(toJson(latencies))
            )
        output.ensureParentDirectory().printWriter().use { it.print(report) }
        logger.info("Mean latency chart is available at ${output.toURI()}")
    }

    private fun toJson(
        latencies: List<CohortMeanLatency>
    ): JsonObject = JSON.createObjectBuilder()
        .add("labels", JSON.createArrayBuilder(latencies.map { prettyPrint(it.cohort) }).build())
        .add("datasets", JSON.createArrayBuilder().add(getDataset(latencies)).build())
        .build()

    private fun getDataset(
        latencies: List<CohortMeanLatency>
    ): JsonObject = JSON.createObjectBuilder()
        .add("label", "Latency experienced by virtual users")
        .add("data", JSON.createArrayBuilder(latencies.map { it.meanLatency }).build())
        .add("backgroundColor", getColor(dataSize = latencies.size, opacity = 0.2))
        .add("borderColor", getColor(dataSize = latencies.size, opacity = 1.0))
        .add("borderWidth", 1)
        .build()

    private fun getColor(dataSize: Int, opacity: Double): JsonArray {
        val normalEntry = "rgba(54, 162, 235, $opacity)"
        val lastEntry = "rgba(75, 192, 192, $opacity)"
        val colorBuilder = JSON.createArrayBuilder()
        (1 until dataSize).forEach { colorBuilder.add(normalEntry) }
        colorBuilder.add(lastEntry)
        return colorBuilder.build()
    }

    private fun prettyPrint(json: JsonObject): String = JsonStyle().prettyPrint(json)

    private fun prettyPrint(cohort: String): String {
        return try {
            val formatter = DateTimeFormatterBuilder()
                .append(ISO_LOCAL_DATE)
                .appendPattern("'T'HH-mm-ss")
                .appendFraction(NANO_OF_SECOND, 0, 3, true)
                .toFormatter()
            LocalDateTime
                .parse(cohort, formatter)
                .format(ofLocalizedDateTime(FormatStyle.SHORT))
        } catch (e: Exception) {
            cohort
        }
    }
}

private data class CohortMeanLatency(
    val cohort: String,
    val meanLatency: Long?
)
