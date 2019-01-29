package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.report.api.result.LocalScalingResult
import org.assertj.core.api.Assertions
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class MultipleCohortsChartTest {

    @Test
    fun shouldOutputHtmlPerAction() {
        val output = Paths.get("build/actual-per-action-latency-chart.html")
        val results = LocalScalingResult(Paths.get("DSR")).loadCohorts()

        MultipleCohortsChart(
            data = results,
            label = { it.action },
            series = { "${it.version} ${it.deployment}"},
            value = { it.value },
            aggregate = { this.average() }
        ).plot(output.toFile())

        val actualOutput = output.toFile()
        val expectedOutput = File(javaClass.getResource("expected-per-action-latency-chart.html").toURI())
        Assertions.assertThat(actualOutput).hasSameContentAs(expectedOutput)
    }

    @Test
    fun shouldOutputHtmlPerDataset() {
        val output = Paths.get("build/actual-per-dataset-chart.html")
        val results = LocalScalingResult(Paths.get("DSR")).loadCohorts()

        MultipleCohortsChart(
            data = results,
            label = { it.dataset.capitalize() },
            series = { "${it.version} ${it.deployment}"},
            value = { it.value},
            aggregate = { this.average() }
        ).plot(output.toFile())

        val actualOutput = output.toFile()
        val expectedOutput = File(javaClass.getResource("expected-per-dataset-chart.html").toURI())
        Assertions.assertThat(actualOutput).hasSameContentAs(expectedOutput)
    }
}
