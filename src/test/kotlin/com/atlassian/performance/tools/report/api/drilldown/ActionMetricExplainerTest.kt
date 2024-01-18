package com.atlassian.performance.tools.report.api.drilldown

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import com.atlassian.performance.tools.report.api.drilldown.ActionMetricExplainer.explainDuration
import com.atlassian.performance.tools.report.chart.waterfall.WaterfallChart
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Test
import java.io.File.createTempFile
import java.time.Duration.ZERO
import java.time.Duration.ofMillis
import kotlin.streams.toList

class ActionMetricExplainerTest {

    private val metricsStream = javaClass.getResourceAsStream("action-metrics-with-elements-and-server.jpt")!!
    private val metrics = ActionMetricsParser().stream(metricsStream).toList()

    @Test
    fun shouldExplainRedirects() {
        // given
        val interestingMetric = metrics.first {
            val drilldown = it.drilldown!!
            val nav = drilldown.navigations.firstOrNull() ?: return@first false
            drilldown.elements.isNotEmpty()
                && nav.redirectCount > 0
                && nav.loadEventEnd != nav.domComplete
        }

        // when
        val interestingDrilldown = explainDuration(interestingMetric)

        // then
        plotWaterfall(interestingMetric)
        assertSoftly {
            with(interestingDrilldown) {
                it.assertThat(preNav).isEqualTo(ofMillis(573).plusNanos(940951)) // huge, right?
                it.assertThat(redirect).isEqualTo(ofMillis(11))
                it.assertThat(serviceWorkerInit).isEqualTo(ZERO)
                it.assertThat(fetchAndCache).isEqualTo(ZERO)
                it.assertThat(dns).isEqualTo(ZERO)
                it.assertThat(tcp).isEqualTo(ZERO)
                it.assertThat(request).isEqualTo(ofMillis(28))
                it.assertThat(response).isEqualTo(ofMillis(57))
                it.assertThat(domProcessing).isEqualTo(ofMillis(158))
                it.assertThat(load).isEqualTo(ofMillis(1))
                it.assertThat(lateResources).isEqualTo(ofMillis(233))
                it.assertThat(excessProcessing).isEqualTo(ofMillis(49).plusNanos(323049))
                it.assertThat(total).isEqualTo(ofMillis(1111).plusNanos(264000))
                it.assertThat(unexplained).isEqualTo(ZERO)
            }
        }
    }

    /**
     * Sometimes UX is measured long after loading the page in the browser,
     * e.g. interact with buttons on an already-loaded page.
     */
    @Test
    fun shouldExplainSinglePageApp() {
        // given
        val spaMetric = metrics.first { it.start > it.drilldown!!.timeOrigin }

        // when
        val drilldown = explainDuration(spaMetric)

        // then
        plotWaterfall(spaMetric)
        assertSoftly {
            with(drilldown) {
                it.assertThat(preNav).isEqualTo(ZERO)
                it.assertThat(redirect).isEqualTo(ZERO)
                it.assertThat(serviceWorkerInit).isEqualTo(ZERO)
                it.assertThat(fetchAndCache).isEqualTo(ZERO)
                it.assertThat(dns).isEqualTo(ZERO)
                it.assertThat(tcp).isEqualTo(ZERO)
                it.assertThat(request).isEqualTo(ZERO)
                it.assertThat(response).isEqualTo(ZERO)
                it.assertThat(domProcessing).isEqualTo(ZERO)
                it.assertThat(load).isEqualTo(ZERO)
                it.assertThat(lateResources).isEqualTo(ofMillis(367).plusNanos(743097))
                it.assertThat(excessProcessing).isEqualTo(ofMillis(60).plusNanos(79903))
                it.assertThat(total).isEqualTo(ofMillis(427).plusNanos(823000))
                it.assertThat(unexplained).isEqualTo(ZERO)
            }
        }
    }

    /**
     * Browser can display the element we need in the middle of many phases, including DCL event or load event.
     */
    @Test
    fun shouldExplainMidNav() {
        // given
        val midNavMetric = metrics.first { metric ->
            val drilldown = metric.drilldown!!
            val timeOrigin = drilldown.timeOrigin!!
            val nav = drilldown.navigations.singleOrNull()?.resource?.entry ?: return@first false
            val navStart = timeOrigin + nav.startTime
            val navEnd = navStart + nav.duration
            metric.end > navStart && metric.end < navEnd
        }

        // when
        val drilldown = explainDuration(midNavMetric)

        // then
        plotWaterfall(midNavMetric)
        assertSoftly {
            with(drilldown) {
                it.assertThat(domProcessing).isEqualTo(ofMillis(205).plusNanos(809903))
                it.assertThat(lateResources).isEqualTo(ZERO)
                it.assertThat(excessProcessing).isEqualTo(ZERO)
                it.assertThat(total).isEqualTo(ofMillis(307).plusNanos(334000))
                it.assertThat(unexplained).isEqualTo(ZERO)
            }
        }
    }

    @Test
    fun shouldExplainDuration() {
        // when
        val drilldowns = metrics.map { it to explainDuration(it) }

        // then
        val unexplained = drilldowns.filter { (_, drilldown) -> drilldown.unexplained != ZERO }
        assertThat(unexplained).isEmpty()
    }

    private fun plotWaterfall(metric: ActionMetric) {
        WaterfallChart().plot(metric, createTempFile("waterfall-${metric.label}-", ".html"))
    }


}
