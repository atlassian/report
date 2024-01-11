package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import org.assertj.core.api.Assertions.assertThat
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
        with(interestingRainbow) {
            assertThat(redirect).isEqualTo(ofMillis(112))
            assertThat(serviceWorkerInit).isEqualTo(ZERO)
            assertThat(fetchAndCache).isEqualTo(ZERO)
            assertThat(dns).isEqualTo(ZERO)
            assertThat(tcp).isEqualTo(ZERO)
            assertThat(request).isEqualTo(ofMillis(33))
            assertThat(response).isEqualTo(ofMillis(90))
            assertThat(processing).isEqualTo(ofMillis(72))
            assertThat(load).isEqualTo(ofMillis(1))
        }
        metrics.forEach { metric ->
            val rainbow = inferRainbow(metric)
            assertThat(rainbow.unexplained).isLessThan(ofMillis(800))
        }
    }

    private fun inferRainbow(metric: ActionMetric): Rainbow {
        val nav = metric.drilldown!!.navigations.single()
        val resource = nav.resource
        return with(resource) {
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
            assert(total.isNegative.not()) { "total duration cannot be negative" }
        }
    }
}
