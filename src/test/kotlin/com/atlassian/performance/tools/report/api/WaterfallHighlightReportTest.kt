package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import com.atlassian.performance.tools.workspace.api.RootWorkspace
import org.junit.Test
import java.nio.file.Paths

class WaterfallHighlightReportTest {

    @Test
    fun shouldReportMedian() {
        val metricsStream = javaClass.getResourceAsStream("/real-action-metrics-with-drilldown.jpt")
        val actionMetrics = ActionMetricsParser().parse(metricsStream)
        val workspace = RootWorkspace(Paths.get("build/jpt-workspace")).currentTask.isolateTest("WaterfallHighlight")
        val highlight = WaterfallHighlightReport()

        highlight.report(actionMetrics, workspace)
    }
}
