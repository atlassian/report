package com.atlassian.performance.tools.report.api.drilldown

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import java.time.Duration

/**
 * Represents [ActionMetric.duration] split into linear segments based on [ActionMetric.drilldown].
 *
 * @since 4.4.0
 */
class DurationDrilldown private constructor(
    /**
     * Everything that happened before current navigation started, including, but not limited to:
     * - delay between measurement start and browser starting to load the page
     * - cross-origin redirects
     * - previous navigations (as within one [ActionMetric] there can be multiple navigations)
     */
    val preNav: Duration,
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
    val domProcessing: Duration,
    val preLoad: Duration,
    val load: Duration,
    /**
     * Resources (XHR, JS, images) still downloading after [load].
     */
    val lateResources: Duration,
    /**
     * Everything that happened after [lateResources]. That is no-network activity, including, but not limited to:
     * - JavaScript execution
     * - rendering
     */
    val excessProcessing: Duration,
    /**
     * @return [ActionMetric.duration]
     */
    val total: Duration
) {

    /**
     * All the segments should add up to [total]
     * @return difference between the sum of segments and the [total], should be zero
     */
    val unexplained: Duration = total
        .minus(preNav)
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
        .minus(domProcessing)
        .minus(lateResources)
        .minus(excessProcessing)
        .minus(preLoad)
        .minus(load)

    init {
        assert(preNav.isNegative.not()) { "preNav duration cannot be negative" }
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
        assert(domProcessing.isNegative.not()) { "processing duration cannot be negative" }
        assert(preLoad.isNegative.not()) { "preLoad duration cannot be negative" }
        assert(load.isNegative.not()) { "load duration cannot be negative" }
        assert(lateResources.isNegative.not()) { "lateResources cannot be negative" }
        assert(excessProcessing.isNegative.not()) { "excessProcessing cannot be negative" }
        assert(total.isNegative.not()) { "total duration cannot be negative" }
    }

    override fun toString(): String {
        return "DurationDrilldown(" +
            "preNav=$preNav, " +
            "redirect=$redirect, " +
            "preWorker=$preWorker, " +
            "serviceWorkerInit=$serviceWorkerInit, " +
            "fetchAndCache=$fetchAndCache, " +
            "dns=$dns, " +
            "preConnect=$preConnect, " +
            "tcp=$tcp, " +
            "preRequest=$preRequest, " +
            "request=$request, " +
            "response=$response, " +
            "domProcessing=$domProcessing, " +
            "preLoad=$preLoad, " +
            "load=$load, " +
            "lateResources=$lateResources, " +
            "excessProcessing=$excessProcessing, " +
            "total=$total, " +
            "unexplained=$unexplained" +
            ")"
    }

    class Builder(
        private var total: Duration
    ) {
        private var preNav: Duration = Duration.ZERO
        private var redirect: Duration = Duration.ZERO
        private var preWorker: Duration = Duration.ZERO
        private var serviceWorkerInit: Duration = Duration.ZERO
        private var fetchAndCache: Duration = Duration.ZERO
        private var dns: Duration = Duration.ZERO
        private var preConnect: Duration = Duration.ZERO
        private var tcp: Duration = Duration.ZERO
        private var preRequest: Duration = Duration.ZERO
        private var request: Duration = Duration.ZERO
        private var response: Duration = Duration.ZERO
        private var domProcessing: Duration = Duration.ZERO
        private var preLoad: Duration = Duration.ZERO
        private var load: Duration = Duration.ZERO
        private var lateResources: Duration = Duration.ZERO
        private var excessProcessing: Duration = Duration.ZERO

        fun preNav(preNav: Duration) = apply { this.preNav = preNav }
        fun redirect(redirect: Duration) = apply { this.redirect = redirect }
        fun preWorker(preWorker: Duration) = apply { this.preWorker = preWorker }
        fun serviceWorkerInit(serviceWorkerInit: Duration) = apply { this.serviceWorkerInit = serviceWorkerInit }
        fun fetchAndCache(fetchAndCache: Duration) = apply { this.fetchAndCache = fetchAndCache }
        fun dns(dns: Duration) = apply { this.dns = dns }
        fun preConnect(preConnect: Duration) = apply { this.preConnect = preConnect }
        fun tcp(tcp: Duration) = apply { this.tcp = tcp }
        fun preRequest(preRequest: Duration) = apply { this.preRequest = preRequest }
        fun request(request: Duration) = apply { this.request = request }
        fun response(response: Duration) = apply { this.response = response }
        fun domProcessing(processing: Duration) = apply { this.domProcessing = processing }
        fun preLoad(preLoad: Duration) = apply { this.preLoad = preLoad }
        fun load(load: Duration) = apply { this.load = load }
        fun lateResources(lateResources: Duration) = apply { this.lateResources = lateResources }
        fun excessProcessing(excessProcessing: Duration) = apply { this.excessProcessing = excessProcessing }
        fun total(total: Duration) = apply { this.total = total }

        fun build() = DurationDrilldown(
            preNav = preNav,
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
            domProcessing = domProcessing,
            preLoad = preLoad,
            load = load,
            lateResources = lateResources,
            excessProcessing = excessProcessing,
            total = total
        )
    }
}

