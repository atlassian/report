package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.ActionMetricsParser
import com.atlassian.performance.tools.workspace.api.git.HardcodedGitRepo
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Paths

class TimelineChartTest {

    private val repo = HardcodedGitRepo(head = "abcd")

    /**
     * The test has very low value as a unit test, but it has a great value as a dev loop booster for report developers.
     */
    @Test
    fun generate() {
        val actionMetrics = ActionMetricsParser().parse(
            javaClass.getResourceAsStream("action-metrics-full.jpt")
        )
        val systemMetrics = SystemMetricsGenerator().asList()
        val reportPath = Paths.get("build/mock-report.html")

        TimelineChart(repo).generate(reportPath, actionMetrics, systemMetrics)

        assertTrue(reportPath.toFile().exists())
    }
}