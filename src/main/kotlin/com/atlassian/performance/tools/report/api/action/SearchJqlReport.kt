package com.atlassian.performance.tools.report.api.action

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import java.nio.file.Path

@Deprecated(
    "Use the open-ended JqlReport.Builder instead",
    ReplaceWith("JqlReport.Builder().build().report(allMetrics, target)")
)
class SearchJqlReport(
    private val allMetrics: List<ActionMetric>
) {

    fun report(target: Path) {
        JqlReport.Builder().build().report(allMetrics, target)
    }
}
