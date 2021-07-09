package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import com.atlassian.performance.tools.report.chart.waterfall.WaterfallChart
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class WaterfallChartTest {

    /**
     * If you want to update the expected HTML, just copy the contents of the file located by the printed path.
     * Don't use IDEA to paste, because it reformats the content. Use a simple text editor or bash.
     */
    @Test
    fun shouldOutputHtml() {
        //given
        val output = Paths.get("build/actual-waterfall-chart.html")
        val inputMetricsResource = "action-metrics-performance-timing.jpt"
        val actionMetrics: List<ActionMetric> =
            ActionMetricsParser().parse(javaClass.getResourceAsStream(inputMetricsResource))
        val metric = actionMetrics[3]

        // when
        WaterfallChart().plot(
            metric = metric,
            output = output.toFile()
        )

        // then
        println("Waterfall chart is available at $output")
        val actualOutput = output.toFile()
        val expectedOutput = File(javaClass.getResource("expected-waterfall-chart.html").toURI())
        Assertions.assertThat(actualOutput).hasSameContentAs(expectedOutput)
    }

    @Test
    fun shouldOutputHtmlForRequestsWithoutPath() {
        //given
        val output = Paths.get("build/actual-waterfall-chart-without-path.html")
        val inputMetricsResource = "search-with-jql.jpt"
        val actionMetrics: List<ActionMetric> =
            ActionMetricsParser().parse(javaClass.getResourceAsStream(inputMetricsResource))
        val metric = actionMetrics[0]

        // when
        WaterfallChart().plot(
            metric = metric,
            output = output.toFile()
        )

        // then
        println("Waterfall chart is available at $output")
        val actualOutput = output.toFile()
        val expectedOutput = File(javaClass.getResource("expected-waterfall-chart-without-path.html").toURI())
        Assertions.assertThat(actualOutput).hasSameContentAs(expectedOutput)
    }

    @Test
    fun shouldNotThrowWhenResponseEndIsBeforeResponseStart() {
        //given
        val output = Paths.get("build/actual-waterfall-chart-with-no-exceptions.html")
        val inputMetricsResource = "action-metrics-with-responseEnd-before-responseStart.jpt"
        val actionMetrics: List<ActionMetric> =
            ActionMetricsParser().parse(javaClass.getResourceAsStream(inputMetricsResource))
        val metric = actionMetrics[8]

        // when
        val noExceptions = Assertions.catchThrowable {
            WaterfallChart().plot(
                metric = metric,
                output = output.toFile()
            )
        }

        // then
        assertThat(noExceptions)
            .`as`("should not throw when responseEnd is before responseStart")
            .doesNotThrowAnyException()
    }

    @Test
    fun shouldNotThrowWhenResourceContainsDataUriScheme() {
        //given
        val output = Paths.get("build/actual-waterfall-chart-with-data-uri-scheme.html")
        val inputMetricsResource = "action-metrics-with-data-uri-scheme.jpt"
        val actionMetrics: List<ActionMetric> =
            ActionMetricsParser().parse(javaClass.getResourceAsStream(inputMetricsResource))
        val metric = actionMetrics[0]

        // when
        val noExceptions = Assertions.catchThrowable {
            WaterfallChart().plot(
                metric = metric,
                output = output.toFile()
            )
        }

        // then
        assertThat(noExceptions)
            .`as`("should not throw when resource URL contains data URI scheme")
            .doesNotThrowAnyException()
    }

    @Test
    fun shouldDoNothingAndNotThrowWhenNavigationsAndResourcesAreEmpty() {
        //given
        val outputFile = Paths.get("build/i-should-not-exist.html").toFile()
        val inputMetricsResource = "action-metrics-with-element-timing-only.jpt"
        val actionMetrics: List<ActionMetric> =
            ActionMetricsParser().parse(javaClass.getResourceAsStream(inputMetricsResource))
        val metric = actionMetrics[0]

        // when
        val noExceptions = Assertions.catchThrowable {
            WaterfallChart().plot(
                metric = metric,
                output = outputFile
            )
        }

        // then
        assertThat(noExceptions)
            .`as`("should not throw when navigations and resources are empty")
            .doesNotThrowAnyException()
        assertThat(outputFile.exists())
            .`as`("should not create any output")
            .isFalse()
    }
}
