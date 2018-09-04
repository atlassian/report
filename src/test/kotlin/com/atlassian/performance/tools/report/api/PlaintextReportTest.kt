package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.ActionMetric
import com.atlassian.performance.tools.jiraactions.ActionMetricStatistics
import com.atlassian.performance.tools.jiraactions.ActionResult.ERROR
import com.atlassian.performance.tools.jiraactions.ActionResult.OK
import com.atlassian.performance.tools.report.api.PlaintextReport
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant.now
import java.util.UUID.randomUUID


class PlaintextReportTest {
    private val actionMetrics = listOf(
        ActionMetric("view", OK, Duration.ofSeconds(1), now(), randomUUID(), null),
        ActionMetric("view", OK, Duration.ofSeconds(2), now(), randomUUID(), null),
        ActionMetric("view", OK, Duration.ofSeconds(3), now(), randomUUID(), null),
        ActionMetric("view", ERROR, Duration.ofSeconds(4), now(), randomUUID(), null),
        ActionMetric("view", ERROR, Duration.ofSeconds(5), now(), randomUUID(), null),
        ActionMetric("create", ERROR, Duration.ofSeconds(1), now(), randomUUID(), null),
        ActionMetric("create", ERROR, Duration.ofSeconds(2), now(), randomUUID(), null),
        ActionMetric("create", ERROR, Duration.ofSeconds(3), now(), randomUUID(), null),
        ActionMetric("login", OK, Duration.ofSeconds(1), now(), randomUUID(), null)
    )

    @Test
    fun testGenerateReport() {
        val report = PlaintextReport(ActionMetricStatistics(actionMetrics)).generate()
        assertEquals(
            """
+---------------------------+---------------+----------+----------------------+
| Action name               | sample size   | errors   | 95th percentile [ms] |
+---------------------------+---------------+----------+----------------------+
| create                    | 0             | 3        | null                 |
| login                     | 1             | 0        | 1000                 |
| view                      | 3             | 2        | 3000                 |
+---------------------------+---------------+----------+----------------------+
""",
            report
        )
    }
}