package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.report.api.impact.LatencyImpactClassifier
import com.atlassian.performance.tools.report.api.impact.LatencyImpactMarkdownTable
import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import java.util.function.Consumer

@Deprecated(
    "use LatencyImpactTable and LatencyImpactClassifier"
)
class LatencyImpactMarkdownTable(
    workspace: TestWorkspace
) : Consumer<LatencyImpact> {

    private val newTable = LatencyImpactMarkdownTable(workspace)
    private val classifier = LatencyImpactClassifier.Builder()
        .handleLatencyImpact(newTable)
        .build()

    override fun accept(newestImpact: LatencyImpact) {
        classifier.accept(newestImpact);
    }
}
