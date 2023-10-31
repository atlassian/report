package com.atlassian.performance.tools.report.jfr

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.w3c.PerformanceResourceTiming
import com.atlassian.performance.tools.report.api.FullTimeline
import com.atlassian.performance.tools.report.api.result.CompressedResult.Companion.unzip
import com.atlassian.performance.tools.report.api.result.RawCohortResult
import jdk.jfr.consumer.RecordedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.function.Predicate

class SlowBackendProfilerTest {
    private val zippedResults = "/real-results" +
        "/aws-cache" +
        "/RegressionCheckIT-post-jql-split-1" +
        "/8.22.0 on DC 2 nodes - baseline, sample 5, 8.22.0, v2-jfr-profiler, DC 2 nodes, class com.atlassian.performance.tools.jiraactions.api.format.MetricVerboseJsonFormat-3.24, 12345" +
        "/sample 5"

    /**
     * The backend profile knows everything the backend was doing.
     * But not everything was slow.
     * So let's keep only the slow parts, removing red herrings.
     */
    @Test
    fun shouldFilterSlowBackendProfile() {
        // given
        val node1Profile = unzip(javaClass, "$zippedResults/jira-node-1/profiler-result.zip")
            .resolve("profiler-result.jfr")
        val node2Profile = unzip(javaClass, "$zippedResults/jira-node-2/profiler-result.zip")
            .resolve("profiler-result.jfr")
        val vuResults = unzip(javaClass, "$zippedResults/virtual-users.zip")
        val slowViewIssueNavigations = RawCohortResult.Factory().fullResult("dummy", vuResults)
            .prepareForJudgement(FullTimeline())
            .actionMetrics
            .filter { it.label == "View Issue" }
            .filter { it.duration > Duration.ofMillis(500) }
            .flatMap { metric ->
                metric
                    .drilldown
                    ?.navigations
                    ?.map { toBackendTimeslot(metric, it.resource) }
                    ?: emptyList()
            }
            .filter { it.duration > Duration.ofMillis(100) }

        // when
        val slotFilter = BackendTimeslotsFilter(slowViewIssueNavigations)
        val profileFilter =  JfrFilter(Predicate(slotFilter::keep))
        val node1ProfileFiltered = profileFilter.filter(node1Profile)
        val node2ProfileFiltered = profileFilter.filter(node2Profile)

        // then
        println("node1ProfileFiltered = $node1ProfileFiltered")
        println("node2ProfileFiltered = $node2ProfileFiltered")
        assertThat(slowViewIssueNavigations).hasSize(67)
    }

    private class BackendTimeslotsFilter(
        private val timeslots: Iterable<BackendTimeslot>
    ) {
        fun keep(profilerEvent: RecordedEvent): Boolean {
            return timeslots.any { slot ->
                slot.contains(profilerEvent.startTime) && slot.threadId == profilerEvent.javaThreadId()
            }
        }
    }

    /**
     * TODO it assumes there's no time slippage between metric and resource
     * TODO we could remove that assumption if the PerformanceResourceTiming (or its parents) would have an Instant
     */
    private fun toBackendTimeslot(metric: ActionMetric, resource: PerformanceResourceTiming) = BackendTimeslot(
        start = metric.start + resource.requestStart,
        end = metric.start + resource.responseEnd,
        threadId = resource
            .serverTiming
            ?.find { it.name == "threadId" }
            ?.description
            ?.toLong()
            ?: throw Exception("No thread id in $metric, so we cannot map it to a backend timeslot")
    )

    class BackendTimeslot(
        val start: Instant,
        val end: Instant,
        val threadId: Long
    ) {
        val duration: Duration = Duration.between(start, end)

        fun contains(instant: Instant): Boolean = instant.isAfter(start) && instant.isBefore(end)
    }

}
