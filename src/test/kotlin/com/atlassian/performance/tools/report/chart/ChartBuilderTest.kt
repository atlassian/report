package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.jiraactions.ActionMetric
import com.atlassian.performance.tools.jiraactions.ActionMetricsParser
import com.atlassian.performance.tools.report.JsonStyle
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.json.Json
import javax.json.JsonObject

class ChartBuilderTest {

    @Test
    fun shouldBuildChart() {
        shouldBuildTheExpectedChart(
            inputMetricsResource = "action-metrics-full.jpt",
            expectedChartResource = "chart-data.json"
        )
    }

    @Test
    fun shouldPlotSlowUsers() {
        shouldBuildTheExpectedChart(
            inputMetricsResource = "action-metrics-with-slow-users.jpt",
            expectedChartResource = "chart-data-with-slow-users.json"
        )
    }

    private fun shouldBuildTheExpectedChart(
        inputMetricsResource: String,
        expectedChartResource: String
    ) {
        val actionMetrics: List<ActionMetric> = ActionMetricsParser().parse(javaClass.getResourceAsStream(inputMetricsResource))
        val sut = ChartBuilder()

        val chart = sut.build(actionMetrics)

        val actualJson = chart.toJson()
        val expectedJson = Json
            .createReader(javaClass.getResourceAsStream(expectedChartResource).bufferedReader())
            .use { it.readObject() }
        assertEquals(prettyPrint(expectedJson), prettyPrint(actualJson))
    }

    private fun prettyPrint(json: JsonObject): String = JsonStyle().prettyPrint(json)
}