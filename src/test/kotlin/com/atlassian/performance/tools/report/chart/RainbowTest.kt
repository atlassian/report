package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import com.atlassian.performance.tools.jiraactions.api.w3c.PerformanceNavigationTiming
import com.atlassian.performance.tools.jiraactions.api.w3c.PerformanceResourceTiming
import com.atlassian.performance.tools.report.chart.waterfall.WaterfallChart
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Test
import java.io.File.createTempFile
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Duration.ofMillis
import java.time.temporal.ChronoUnit.MILLIS
import kotlin.streams.toList

class RainbowTest {

    private val metricsStream = javaClass.getResourceAsStream("action-metrics-with-elements-and-server.jpt")!!
    private val metrics = ActionMetricsParser().stream(metricsStream).toList()

    @Test
    fun shouldCalculateRainbowWithNavigation() {
        // given
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
        plotWaterfall(interestingMetric)
        assertSoftly {
            with(interestingRainbow) {
                it.assertThat(redirect).isEqualTo(ofMillis(11))
                it.assertThat(serviceWorkerInit).isEqualTo(ZERO)
                it.assertThat(fetchAndCache).isEqualTo(ZERO)
                it.assertThat(dns).isEqualTo(ZERO)
                it.assertThat(tcp).isEqualTo(ZERO)
                it.assertThat(request).isEqualTo(ofMillis(28))
                it.assertThat(response).isEqualTo(ofMillis(57))
                it.assertThat(processing).isEqualTo(ofMillis(158))
                it.assertThat(load).isEqualTo(ofMillis(1))
                it.assertThat(excessResource).isEqualTo(ofMillis(233))
                it.assertThat(excessJavascript.truncatedTo(MILLIS)).isEqualTo(ofMillis(623))
                it.assertThat(total).isEqualTo(ofMillis(1111).plusNanos(264000))
                it.assertThat(unexplained).isBetween(ZERO, ofMillis(1))
            }
        }
    }

    /**
     * Sometimes UX is measured long after loading the page in the browser,
     * e.g. interact with buttons on an already-loaded page.
     */
    @Test
    fun shouldCalculateRainbowForSinglePageApp() {
        // given
        val spaMetric = metrics.first { it.start > it.drilldown!!.timeOrigin }

        // when
        val rainbow = inferRainbow(spaMetric)

        // then
        plotWaterfall(spaMetric)
        assertSoftly {
            with(rainbow) {
                it.assertThat(redirect).isEqualTo(ZERO)
                it.assertThat(serviceWorkerInit).isEqualTo(ZERO)
                it.assertThat(fetchAndCache).isEqualTo(ZERO)
                it.assertThat(dns).isEqualTo(ZERO)
                it.assertThat(tcp).isEqualTo(ZERO)
                it.assertThat(request).isEqualTo(ZERO)
                it.assertThat(response).isEqualTo(ZERO)
                it.assertThat(processing).isEqualTo(ZERO)
                it.assertThat(load).isEqualTo(ZERO)
                it.assertThat(excessResource).isNotEqualTo(ZERO)
                it.assertThat(total).isEqualTo(ofMillis(427).plusNanos(823000))
                it.assertThat(unexplained).isBetween(ZERO, ofMillis(1))
            }
        }
    }

    @Test
    fun shouldExplainAlmostEverything() {
        // when
        val rainbows = metrics.map { inferRainbow(it) }

        // then
        val unexplained = rainbows.filter { it.unexplained < ZERO || it.unexplained > ofMillis(1) }
        assertThat(unexplained.size).isLessThan(103)
    }

    private fun plotWaterfall(metric: ActionMetric) {
        WaterfallChart().plot(metric, createTempFile("waterfall-${metric.label}-", ".html"))
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
            val lastResource = metric.drilldown!!.resources
                .map { it.responseEnd }
                .filter { it < metric.duration }
                .max() ?: ZERO
            val excessResource = train.jumpOff(lastResource)
            val excessJavascript = train.jumpOff(metric.duration)
            Rainbow(
                redirect = redirect,
                preWorker = preWorker,
                serviceWorkerInit = serviceWorkerInit,
                fetchAndCache = fetchAndCache,
                dns = dns,
                preConnect = preConnect,
                tcp = tcp,
                preRequest = preRequest,
                request = request,
                response = response,
                processing = processing,
                preLoad = preLoad,
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
        val preWorker: Duration,
        val serviceWorkerInit: Duration,
        val fetchAndCache: Duration,
        val dns: Duration,
        val preConnect: Duration,
        val tcp: Duration,
        val preRequest: Duration,
        val request: Duration,
        val response: Duration,
        val processing: Duration,
        val preLoad: Duration,
        val load: Duration,
        val excessResource: Duration,
        val excessJavascript: Duration,
        val total: Duration
    ) {

        val unexplained: Duration = total
            .minus(redirect)
            .minus(preWorker)
            .minus(serviceWorkerInit)
            .minus(fetchAndCache)
            .minus(dns)
            .minus(preConnect)
            .minus(tcp)
            .minus(preRequest)
            .minus(request)
            .minus(response)
            .minus(processing)
            .minus(excessResource)
            .minus(excessJavascript)
            .minus(preLoad)
            .minus(load)

        init {
            assert(redirect.isNegative.not()) { "redirect duration cannot be negative" }
            assert(preWorker.isNegative.not()) { "preWorker duration cannot be negative" }
            assert(serviceWorkerInit.isNegative.not()) { "serviceWorkerInit duration cannot be negative" }
            assert(fetchAndCache.isNegative.not()) { "fetchAndCache duration cannot be negative" }
            assert(dns.isNegative.not()) { "dns duration cannot be negative" }
            assert(preConnect.isNegative.not()) { "preConnect duration cannot be negative" }
            assert(tcp.isNegative.not()) { "tcp duration cannot be negative" }
            assert(preRequest.isNegative.not()) { "preRequest duration cannot be negative" }
            assert(request.isNegative.not()) { "request duration cannot be negative" }
            assert(response.isNegative.not()) { "response duration cannot be negative" }
            assert(processing.isNegative.not()) { "processing duration cannot be negative" }
            assert(preLoad.isNegative.not()) { "preLoad duration cannot be negative" }
            assert(load.isNegative.not()) { "load duration cannot be negative" }
            assert(excessResource.isNegative.not()) { "excessResource cannot be negative" }
            assert(excessJavascript.isNegative.not()) { "excessJavascript cannot be negative" }
            assert(total.isNegative.not()) { "total duration cannot be negative" }
        }
    }
}
