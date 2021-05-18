package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionMetricStatistics
import com.atlassian.performance.tools.jiraactions.api.ActionResult.ERROR
import com.atlassian.performance.tools.jiraactions.api.ActionResult.OK
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration.ofSeconds
import java.time.Instant.now
import java.util.UUID.randomUUID


class CSVReportTest {
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
        val report = CSVReport(ActionMetricStatistics(actionMetrics)).generate()
        assertEquals(
            """actionName,sampleSize,errors,total,p50,p95,p99,max
a very very very long action name,1,0,1,1000,1000,1000,1000
create,0,3,3,,,,
login,1,0,1,1000,1000,1000,1000
view,3,2,5,2000,3000,3000,3000
""",
            report
        )
    }
}
