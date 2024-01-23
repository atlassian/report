package com.atlassian.performance.tools.report.api.drilldown

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.report.drilldown.TimeTrain
import java.time.Instant

/**
 * @since 4.4.0
 */
object ActionMetricExplainer {
    fun explainDuration(metric: ActionMetric): DurationDrilldown {
        val drilldown = metric.drilldown!!
        val nav = drilldown.navigations.single()
        val timeOrigin = drilldown.timeOrigin!!
        return with(nav.resource) {
            val train = TimeTrain(metric.start, metric.end, timeOrigin)
            val lastResource = drilldown.resources
                .map { timeOrigin + it.responseEnd }
                .filter { it < metric.end }
                .max() ?: Instant.MIN

            DurationDrilldown.Builder(metric.duration)
                .preNav(train.jumpOff(redirectStart))
                .redirect(train.jumpOff(redirectEnd))
                .preWorker(train.jumpOff(workerStart))
                .serviceWorkerInit(train.jumpOff(fetchStart))
                .fetchAndCache(train.jumpOff(domainLookupStart))
                .dns(train.jumpOff(domainLookupEnd))
                .preConnect(train.jumpOff(connectStart))
                .tcp(train.jumpOff(connectEnd))
                .preRequest(train.jumpOff(requestStart))
                .request(train.jumpOff(responseStart))
                .response(train.jumpOff(responseEnd))
                .domProcessing(train.jumpOff(nav.domComplete))
                .preLoad(train.jumpOff(nav.loadEventStart))
                .load(train.jumpOff(nav.loadEventEnd))
                .lateResources(train.jumpOff(lastResource))
                .excessProcessing(train.jumpOff(metric.end))
                .build()
        }
    }
}
