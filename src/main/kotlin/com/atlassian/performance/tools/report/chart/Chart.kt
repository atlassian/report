package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.report.JsonProviderSingleton.JSON
import javax.json.JsonArrayBuilder
import javax.json.JsonObject

internal class Chart<X>(
    private val lines: List<ChartLine<X>>
) where X : Comparable<X> {

    fun toJson(): JsonObject {
        val dataBuilder = JSON.createObjectBuilder()
        dataBuilder.add("labels", JSON.createArrayBuilder(getLabels()).build())
        dataBuilder.add("datasets", getLines())
        return dataBuilder.build()
    }

    private fun getLines(): JsonArrayBuilder {
        val linesBuilder = JSON.createArrayBuilder()
        lines.forEach {
            linesBuilder.add(it.toJson())
        }
        return linesBuilder
    }

    private fun getLabels(): Set<String> = lines
        .flatMap { it.data }
        .sortedBy { it.x }
        .map { it.labelX() }
        .toSet()
}
