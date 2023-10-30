package com.atlassian.performance.tools.report.jfr

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.w3c.PerformanceResourceTiming
import com.atlassian.performance.tools.report.api.FullTimeline
import com.atlassian.performance.tools.report.api.result.CompressedResult.Companion.unzip
import com.atlassian.performance.tools.report.api.result.RawCohortResult
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
        val node2Profile = unzip(javaClass, "$zippedResults/jira-node-2/profiler-result.zip") // TODO what about jira-node-1? we need to merge profiles from all nodes, but also thread ids will collide, we can map ActionMetric.virtualUser.toString by EdibleResult.nodeDistribution to match nodes
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
            .filter { it.duration > Duration.ofMillis(400) } // very aggressive filter just to see if the output is actually filtered, TODO can be relaxed when filtering by threadid is done
        val filter = BackendTimeslotsFilter(slowViewIssueNavigations)

        // when
        val output = JfrExecutionEventFilter(Predicate(filter::keep)).filter(node2Profile)

        // then
        println("output = $output")
        assertThat(slowViewIssueNavigations).hasSize(3)
    }

    private class BackendTimeslotsFilter(
        private val timeslots: Iterable<BackendTimeslot>
    ) {
        fun keep(profilerEvent: Event): Boolean {
            return timeslots.any { slot ->
                if (slot.threadId == null) {
                    throw Exception("No thread id in $slot, cannot filter out red herrings")
                }
                slot.contains(profilerEvent.start) // && slot.threadId == profilerEvent.threadId
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
            ?.toInt()
    )

    class BackendTimeslot(
        val start: Instant,
        val end: Instant,
        val threadId: Int? // TODO or throw instead of null
    ) {
        val duration: Duration = Duration.between(start, end)

        fun contains(instant: Instant): Boolean = instant.isAfter(start) && instant.isBefore(end)
    }

}
