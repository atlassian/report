package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.report.JsonStyle
import com.atlassian.performance.tools.report.ui.Adg
import org.apache.logging.log4j.LogManager
import java.io.File
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject

class MultipleCohortsChart<DataPoint>(
    private val data: List<DataPoint>,
    private val label: (DataPoint) -> String,
    private val series: (DataPoint) -> String,
    private val value: (DataPoint) -> Double,
    private val aggregate: List<Double>.() -> Double
) {
    private inner class Series(
        val name: String,
        val data: Map<String, List<DataPoint>>
    ) {
        fun dataFor(label: String): Double? = data[label]?.map { value(it) }?.aggregate()
    }

    private val labels = data.map { label(it) }.toSortedSet()

    private fun chartData(): List<Series> {
        return data
            .groupBy { series(it) }
            .map { (name, data) -> Series(name, data.groupBy { label(it) }) }
            .sortedBy { it.name }
    }

    private val logger = LogManager.getLogger(this::class.java)

    fun plot(
        output: File
    ) {
        val report = this::class
            .java
            .getResourceAsStream("per-action-chart-template.html")
            .bufferedReader()
            .use { it.readText() }
            .replace(
                oldValue = "'<%= data =%>'",
                newValue = JsonStyle().prettyPrint(toJson())
            )
        output.ensureParentDirectory().printWriter().use { it.print(report) }
        logger.debug("Per action latency chart is available at ${output.toURI()}")
    }

    private fun toJson(): JsonObject {
        val labelBuilder = Json.createArrayBuilder()
        labels.forEach { labelBuilder.add(it.toString()) }

        val dataBuilder = Json.createArrayBuilder()
        chartData().mapIndexed { index, dataSeries -> toJson(index, dataSeries) }.forEach { dataBuilder.add(it) }

        return Json.createObjectBuilder()
            .add("labels", labelBuilder.build())
            .add("datasets", dataBuilder.build())
            .build()
    }

    private fun toJson(
        index: Int,
        series: Series
    ): JsonObject {
        return Json.createObjectBuilder()
            .add("label", series.name)
            .add("backgroundColor", getColor(index, labels.size))
            .add("data", toJson(series))
            .build()
    }

    private fun getColor(
        index: Int,
        dataSize: Int
    ): JsonArray {
        val colors = Adg().colors
        if (index > colors.size) {
            return Json.createArrayBuilder().build()
        }
        val builder = Json.createArrayBuilder()
        repeat(dataSize) {
            builder.add(colors[index])
        }
        return builder.build()
    }

    private fun toJson(
        stats: Series
    ): JsonArray {
        val builder = Json.createArrayBuilder()
        labels
            .map { label -> stats.dataFor(label) }
            .forEach { builder.add(it?.toLong() ?: 0L) }

        return builder.build()
    }
}