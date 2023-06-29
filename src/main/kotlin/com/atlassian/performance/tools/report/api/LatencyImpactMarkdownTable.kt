package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import java.util.function.Consumer

class LatencyImpactMarkdownTable(
    workspace: TestWorkspace
) : Consumer<LatencyImpact> {
    override fun accept(impact: LatencyImpact) {
    }
}
