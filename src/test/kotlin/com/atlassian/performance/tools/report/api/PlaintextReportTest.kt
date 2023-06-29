package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionMetricStatistics
import com.atlassian.performance.tools.jiraactions.api.ActionResult.ERROR
import com.atlassian.performance.tools.jiraactions.api.ActionResult.OK
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration.ofSeconds
import java.time.Instant.now


class PlaintextReportTest {
    private val actionMetrics = listOf(
        ActionMetric.Builder("a very very very long action name", OK, ofSeconds(1), now()).build(),
        ActionMetric.Builder("view", OK, ofSeconds(1), now()).build(),
        ActionMetric.Builder("view", OK, ofSeconds(2), now()).build(),
        ActionMetric.Builder("view", OK, ofSeconds(3), now()).build(),
        ActionMetric.Builder("view", ERROR, ofSeconds(4), now()).build(),
        ActionMetric.Builder("view", ERROR, ofSeconds(5), now()).build(),
        ActionMetric.Builder("create", ERROR, ofSeconds(1), now()).build(),
        ActionMetric.Builder("create", ERROR, ofSeconds(2), now()).build(),
        ActionMetric.Builder("create", ERROR, ofSeconds(3), now()).build(),
        ActionMetric.Builder("login", OK, ofSeconds(1), now()).build()
    )

    @Test
    fun testGenerateReport() {
        val report = PlaintextReport(ActionMetricStatistics(actionMetrics)).generate()
        assertEquals(
            """
+---------------------------+---------------+----------+----------------------+
| Action name               | sample size   | errors   | 95th percentile [ms] |
+---------------------------+---------------+----------+----------------------+
| a very very very long ... | 1             | 0        | 1000                 |
| create                    | 0             | 3        | null                 |
| login                     | 1             | 0        | 1000                 |
| view                      | 3             | 2        | 3000                 |
+---------------------------+---------------+----------+----------------------+
""",
            report
        )
    }
}
