package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Duration.ofMillis
import kotlin.streams.toList

class RainbowTest {

    @Test
    fun shouldCalculateRainbowWithNavigation() {
        // given
        val metricsStream = javaClass.getResourceAsStream("action-metrics-with-elements-and-server.jpt")!!
        val metrics = ActionMetricsParser().stream(metricsStream).toList()
        val interestingMetric = metrics.first {
            val drilldown = it.drilldown!!
            val nav = drilldown.navigations.firstOrNull() ?: return@first false
            drilldown.elements.isNotEmpty()
                && nav.redirectCount > 0
                && nav.loadEventEnd != nav.domComplete
        }

        // when
        val interestingRainbow = inferRainbow(interestingMetric)

        // then
        SoftAssertions.assertSoftly {
            with(interestingRainbow) {
                it.assertThat(redirect).isEqualTo(ofMillis(112))
                it.assertThat(serviceWorkerInit).isEqualTo(ZERO)
                it.assertThat(fetchAndCache).isEqualTo(ZERO)
                it.assertThat(dns).isEqualTo(ZERO)
                it.assertThat(tcp).isEqualTo(ZERO)
                it.assertThat(request).isEqualTo(ofMillis(33))
                it.assertThat(response).isEqualTo(ofMillis(90))
                it.assertThat(processing).isEqualTo(ofMillis(72))
                it.assertThat(load).isEqualTo(ofMillis(1))
                it.assertThat(excessResource).isEqualTo(ofMillis(184))
                it.assertThat(total).isEqualTo(ofMillis(546).plusNanos(821000))
                it.assertThat(unexplained).isLessThan(ofMillis(100))
            }

        }
        metrics.forEach { metric ->
            val rainbow = inferRainbow(metric)
            assertThat(rainbow.unexplained).isLessThan(ofMillis(100))
        }
    }

    private fun inferRainbow(metric: ActionMetric): Rainbow {
        val nav = metric.drilldown!!.navigations.single()
        val resources = metric.drilldown!!.resources
        val excessResource = resources.map { it.responseEnd }.max()!!
        return with(nav.resource) {
            Rainbow(
                redirect = redirectEnd - redirectStart,
                serviceWorkerInit = if (workerStart != ZERO) fetchStart - workerStart else ZERO,
                fetchAndCache = domainLookupStart - fetchStart,
                dns = domainLookupEnd - domainLookupEnd,
                tcp = connectEnd - connectStart,
                request = responseStart - requestStart,
                response = responseEnd - responseStart,
                processing = nav.domComplete - responseEnd,
                load = nav.loadEventEnd - nav.loadEventStart,
                excessResource = if (excessResource > nav.loadEventEnd) excessResource - nav.loadEventEnd else ZERO,
                total = metric.duration
            )
        }
    }

    /**
     * TODO find a better name
     * it's temporarily a rainbow, because the old visualisation of similar data looked like one
     */
    class Rainbow(
        val redirect: Duration,
        val serviceWorkerInit: Duration,
        val fetchAndCache: Duration,
        val dns: Duration,
        val tcp: Duration,
        val request: Duration,
        val response: Duration,
        val processing: Duration,
        val load: Duration,
        val excessResource: Duration,
        val total: Duration
    ) {

        val unexplained: Duration = total
            .minus(redirect)
            .minus(serviceWorkerInit)
            .minus(fetchAndCache)
            .minus(dns)
            .minus(tcp)
            .minus(request)
            .minus(response)
            .minus(processing)
            .minus(excessResource)
            .minus(load)

        init {
            assert(redirect.isNegative.not()) { "redirect duration cannot be negative" }
            assert(serviceWorkerInit.isNegative.not()) { "serviceWorkerInit duration cannot be negative" }
            assert(fetchAndCache.isNegative.not()) { "fetchAndCache duration cannot be negative" }
            assert(dns.isNegative.not()) { "dns duration cannot be negative" }
            assert(tcp.isNegative.not()) { "tcp duration cannot be negative" }
            assert(request.isNegative.not()) { "request duration cannot be negative" }
            assert(response.isNegative.not()) { "response duration cannot be negative" }
            assert(processing.isNegative.not()) { "processing duration cannot be negative" }
            assert(load.isNegative.not()) { "load duration cannot be negative" }
            assert(excessResource.isNegative.not()) { "excessResource cannot be negative" }
            assert(total.isNegative.not()) { "total duration cannot be negative" }
        }
    }
}
