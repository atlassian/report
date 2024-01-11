package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import com.atlassian.performance.tools.jiraactions.api.w3c.PerformanceNavigationTiming
import com.atlassian.performance.tools.jiraactions.api.w3c.PerformanceResourceTiming
import com.atlassian.performance.tools.report.chart.waterfall.WaterfallChart
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Duration.ofMillis
import java.time.temporal.ChronoUnit.MILLIS
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
                it.assertThat(excessJavascript.truncatedTo(MILLIS)).isEqualTo(ofMillis(53))
                it.assertThat(total).isEqualTo(ofMillis(546).plusNanos(821000))
                it.assertThat(unexplained).isLessThan(ofMillis(100))
            }

        }
        val maxUnexplained = metrics.maxBy { inferRainbow(it).unexplained }!!
        WaterfallChart().plot(maxUnexplained, File("/tmp/max-waterfall.html"))
        metrics.forEach { metric ->
            val rainbow = inferRainbow(metric)
            assertThat(rainbow.unexplained).isLessThan(ofMillis(100))
        }
    }

    private fun inferRainbow(metric: ActionMetric): Rainbow {
        val nav = metric.drilldown!!.navigations.single()
        return with(nav.resource) {
            val train = TimeTrain(redirectStart)
            val redirect = train.jumpOff(redirectEnd)
            val preWorker = train.jumpOff(workerStart)
            val serviceWorkerInit = train.jumpOff(fetchStart)
            val fetchAndCache = train.jumpOff(domainLookupStart)
            val dns = train.jumpOff(domainLookupEnd)
            val preConnect = train.jumpOff(connectStart)
            val tcp = train.jumpOff(connectEnd)
            val preRequest = train.jumpOff(requestStart)
            val request = train.jumpOff(responseStart)
            val response = train.jumpOff(responseEnd)
            val processing = train.jumpOff(nav.domComplete)
            val preLoad = train.jumpOff(nav.loadEventStart)
            val load = train.jumpOff(nav.loadEventEnd)
            val lastResource = metric.drilldown!!.resources.map { it.responseEnd }.max()
            val excessResource = train.jumpOff(lastResource ?: ZERO)
            val excessJavascript = train.jumpOff(metric.duration)
            Rainbow(
                redirect = redirect,
                serviceWorkerInit = serviceWorkerInit,
                fetchAndCache = fetchAndCache,
                dns = dns,
                tcp = tcp,
                request = request,
                response = response,
                processing = processing,
                load = load,
                excessResource = excessResource,
                excessJavascript = excessJavascript,
                total = metric.duration
            )
        }
    }

    class TimeTrain(
        private var lastStation: Duration
    ) {
        /**
         * Jump off at the next stop
         *
         * @return how much time elapsed since the last stop, a linear time segment
         */
        fun jumpOff(nextStation: Duration): Duration {
            /**
             * Some stations are optional, e.g. [PerformanceResourceTiming.workerStart]
             * or might not have happened yet, e.g. before [PerformanceNavigationTiming.loadEventStart]
             */
            if (nextStation == ZERO) {
                return ZERO
            }
            /**
             * Some stations are parallel and can come in different order in runtime,
             * e.g. [PerformanceNavigationTiming.unloadEventEnd] might come before or after [PerformanceResourceTiming.redirectEnd].
             */
            if (nextStation < lastStation) {
                return ZERO
            }
            val segment = nextStation - lastStation
            lastStation = nextStation
            return segment
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
        val excessJavascript: Duration,
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
            .minus(excessJavascript)
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
            assert(excessJavascript.isNegative.not()) { "excessJavascript cannot be negative" }
            assert(total.isNegative.not()) { "total duration cannot be negative" }
        }
    }
}
