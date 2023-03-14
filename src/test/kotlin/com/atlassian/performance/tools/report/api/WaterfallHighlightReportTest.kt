package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import com.atlassian.performance.tools.workspace.api.RootWorkspace
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.file.Paths
import kotlin.streams.toList

class WaterfallHighlightReportTest {

    @Test
    fun shouldReportMedian() {
        report("/real-action-metrics-with-drilldown.jpt")
    }

    @Test
    fun shouldHighlightIn2023() {
        report("/real-action-metrics-with-drilldown-2023.jpt")
    }

    private fun report(resourceName: String) {
        val metricsStream = javaClass.getResourceAsStream(resourceName)!!
        val actionMetrics = ActionMetricsParser().stream(metricsStream).toList()
        val workspace = RootWorkspace(Paths.get("build/jpt-workspace"))
            .currentTask
            .isolateTest("WaterfallHighlight/$resourceName")
        val highlight = WaterfallHighlightReport()

        highlight.report(actionMetrics, workspace)

        val hasViewIssue = workspace.directory.toFile().walkTopDown().any { it.name == "View Issue" }
        assertThat(hasViewIssue)
            .`as`("$workspace has View Issue")
            .isTrue()
    }
}
