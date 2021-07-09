package com.atlassian.performance.tools.report.chart.waterfall

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.w3c.PerformanceNavigationTiming
import com.atlassian.performance.tools.jiraactions.api.w3c.PerformanceResourceTiming
import com.atlassian.performance.tools.report.chart.waterfall.Phase.*
import org.apache.logging.log4j.LogManager
import java.io.File
import java.time.Duration
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject

internal class WaterfallChart {

    private val logger = LogManager.getLogger(this::class.java)

    fun plot(
        metric: ActionMetric,
        output: File
    ) {
        val performance = metric.drilldown
        if (performance == null) {
            logger.debug("Drilldown for ${metric.label} is missing, so waterfall chart is skipped.")
            return
        }

        if(performance.navigations.isEmpty() or performance.resources.isEmpty()) {
            logger.debug("No navigations or resources data in the drilldown data, so waterfall chart is skipped.")
            return
        }

        val requests = performance.navigations.map { toProcessingModel(it) }.toMutableList()
        requests.addAll(
            performance.resources.map { toProcessingModel(it) }
        )
        plot(
            actionLabel = metric.label,
            duration = metric.duration,
            domLoaded = performance.navigations[0].domContentLoadedEventEnd,
            loadEnd = performance.navigations[0].loadEventEnd,
            requests = requests,
            output = output
        )
    }

    private fun plot(
        actionLabel: String,
        duration: Duration,
        domLoaded: Duration,
        loadEnd: Duration,
        requests: Collection<ProcessingModel>,
        output: File
    ) {
        val report = this::class
            .java
            .getResourceAsStream("waterfall-chart-template.html")
            .bufferedReader()
            .use { it.readText() }
            .replace(
                oldValue = "'<%= waterfallChartData =%>'",
                newValue = Utils().prettyPrint(toJson(requests))
            )
            .replace(
                oldValue = "'<%= waterfallChartTitle =%>'",
                newValue = Utils().prettyPrint(
                    getTitle(
                        actionLabel = actionLabel,
                        requests = requests,
                        duration = duration
                    )
                )
            )
            .replace(
                oldValue = "'<%= waterfallChartDOMLoadedValue =%>'",
                newValue = domLoaded.toMillis().toString()
            )
            .replace(
                oldValue = "'<%= waterfallChartLoadEndValue =%>'",
                newValue = loadEnd.toMillis().toString()
            )

        output.ensureParentDirectory().printWriter().use { it.print(report) }
        logger.info("Waterfall chart is available at ${output.toURI()}")
    }

    private fun toProcessingModel(navigation: PerformanceNavigationTiming): ProcessingModel {
        val resource = navigation.resource
        // Sometimes domComplete equals to 0, while domInteractive != 0
        // This looks like a bug, we will treat domComplete as equal to domContentLoadedEventEnd in such case
        val domComplete = when(navigation.domComplete.isZero) {
            true -> navigation.domContentLoadedEventEnd
            false -> navigation.domComplete
        }
        val phases = resourceToStack(resource)
            .push(
                P11_DOM_PARSING,
                P12_PROCESSING,
                navigation.domInteractive,
                domComplete
            )
            .push(
                P13_IDLE,
                P14_LOAD,
                navigation.loadEventStart,
                navigation.loadEventEnd
            )
            .build()

        return ProcessingModel(
            address = resource.entry.name,
            phases = phases,
            initiatorType = resource.initiatorType,
            transferSize = resource.transferSize,
            decodedBodySize = resource.decodedBodySize,
            totalDuration = resource.entry.duration
        )
    }

    private fun toProcessingModel(resource: PerformanceResourceTiming): ProcessingModel {
        val phases = resourceToStack(resource)
            .push(
                P11_DOM_PARSING,
                P12_PROCESSING,
                Duration.ZERO,
                Duration.ZERO
            )
            .push(
                P13_IDLE,
                P14_LOAD,
                Duration.ZERO,
                Duration.ZERO
            )
            .build()

        return ProcessingModel(
            address = resource.entry.name,
            phases = phases,
            initiatorType = resource.initiatorType,
            transferSize = resource.transferSize,
            decodedBodySize = resource.decodedBodySize,
            totalDuration = resource.entry.duration
        )
    }

    private fun resourceToStack(resource: PerformanceResourceTiming): PhaseStackBuilder {
        // special case, see paragraph 9. of 4.6.1 Processing Model:  https://www.w3.org/TR/resource-timing-2/#processing-model
        val timingAllowCheckFailed = resource.domainLookupStart.isZero &&
            resource.domainLookupEnd.isZero &&
            resource.connectStart.isZero &&
            resource.connectEnd.isZero &&
            resource.requestStart.isZero &&
            resource.responseStart.isZero
        // Sometimes responseEnd equals to 0, while responseStart != 0, effectively putting responseEnd event BEFORE responseStart event
        // As far as I can read https://www.w3.org/TR/resource-timing-2/ logic it should never happen
        // so it looks like a bug, we will treat responseEnd as equal to responseStart in such case
        val responseEnd = when(resource.responseEnd.isZero && !resource.requestStart.isZero) {
            true -> resource.responseStart
            false -> resource.responseEnd
        }

        return PhaseStackBuilder()
            .push(
                null,
                P0_IDLE,
                Duration.ZERO,
                resource.entry.startTime
            )
            .push(
                null,
                P1_REDIRECT,
                resource.redirectStart,
                resource.redirectEnd
            )
            .push(
                P2_IDLE,
                P3_APPCHACHE,
                resource.fetchStart,
                when (timingAllowCheckFailed) {
                    true -> resource.responseEnd
                    false -> resource.domainLookupStart
                }
            )
            .push(
                null,
                P4_DNS,
                resource.domainLookupStart,
                resource.domainLookupEnd
            )
            .push(
                P5_IDLE,
                P6_TCP,
                resource.connectStart,
                when (resource.secureConnectionStart.isZero) {
                    true -> resource.connectEnd
                    false -> resource.secureConnectionStart
                }
            )
            .push(
                null,
                P7_SSL,
                resource.secureConnectionStart,
                when (resource.secureConnectionStart.isZero) {
                    true -> Duration.ZERO
                    false -> resource.connectEnd
                }
            )
            .push(
                P8_IDLE,
                P9_REQUEST,
                resource.requestStart,
                resource.responseStart
            )
            .push(
                null,
                P10_RESPONSE,
                when (timingAllowCheckFailed) {
                    true -> resource.responseEnd
                    false -> resource.responseStart
                },
                responseEnd
            )
    }

    private fun getTitle(
        actionLabel: String,
        requests: Collection<ProcessingModel>,
        duration: Duration
    ): JsonObject {
        val size = Utils().toHumanReadableSize(requests.map { it.transferSize }.sum())
        return Json.createObjectBuilder()
            .add("display", true)
            .add("text", Json.createArrayBuilder(listOf(
                "\"$actionLabel\" action requests waterfall chart",
                "requests: ${requests.size}, total duration: ${duration.toMillis()} ms, total transfer: $size"
            )).build())
            .build()
    }

    private fun toJson(
        requests: Collection<ProcessingModel>
    ): JsonObject = Json.createObjectBuilder()
        .add("labels", Json.createArrayBuilder(requests.map { Utils().prettyPrint(it.address) }).build())
        .add("fullLabels", Json.createArrayBuilder(requests.map { it.address }).build())
        .add("initiatorTypes", Json.createArrayBuilder(requests.map { it.initiatorType }).build())
        .add("transferSizes", Json.createArrayBuilder(requests.map { it.transferSize }).build())
        .add("decodedBodySizes", Json.createArrayBuilder(requests.map { it.decodedBodySize }).build())
        .add("totalDurations", Json.createArrayBuilder(requests.map { it.totalDuration.toMillis() }).build())
        .add("datasets", getDatasets(requests))
        .build()

    private fun getDatasets(
        requests: Collection<ProcessingModel>
    ): JsonArray {
        val datasetBuilder = Json.createArrayBuilder()
        enumValues<Phase>().forEach {
            datasetBuilder.add(getDataset(it, requests))
        }
        return datasetBuilder.build()
    }

    private fun getDataset(
        phase: Phase,
        requests: Collection<ProcessingModel>
    ): JsonObject = Json.createObjectBuilder()
        .add("label", phase.label)
        .add("backgroundColor", phase.color)
        .add("data", Json.createArrayBuilder(requests.map { it.phases[phase]!!.toMillis() }).build())
        .build()
}
