package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.report.Point
import java.util.*
import javax.json.Json
import javax.json.JsonObject

internal class ChartLine<X>(
    val data: List<Point<X>>,
    private val label: String,
    private val type: String,
    private val yAxisId: String,
    private val hidden: Boolean = false,
    private val cohort: String = ""
) where X : Comparable<X> {
    fun toJson(): JsonObject {
        val dataBuilder = Json.createArrayBuilder()
        data.forEach { point ->
            dataBuilder.add(
                Json.createObjectBuilder()
                    .add("x", point.labelX())
                    .add("y", point.y)
                    .build()
            )
        }
        val chartDataBuilder = Json.createObjectBuilder()
        chartDataBuilder.add("type", type)
        chartDataBuilder.add("label", label)

        if (!cohort.isBlank()) {
            chartDataBuilder.add("cohort", cohort)
        }

        chartDataBuilder.add("borderColor", getColor(label))
        chartDataBuilder.add("backgroundColor", getColor(label))
        chartDataBuilder.add("fill", false)
        chartDataBuilder.add("data", dataBuilder)
        chartDataBuilder.add("yAxisID", yAxisId)
        chartDataBuilder.add("hidden", hidden)
        chartDataBuilder.add("lineTension", 0)

        return chartDataBuilder.build()
    }

    private fun getColor(label: String): String {
        val random = Random(label.hashCode().toLong())

        val g = random.nextInt(255)
        val r = random.nextInt(255)
        val b = random.nextInt(255)

        return "rgb($r, $g, $b)"
    }
}