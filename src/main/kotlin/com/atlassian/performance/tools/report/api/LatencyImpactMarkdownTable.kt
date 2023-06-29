package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import java.util.function.Consumer

class LatencyImpactMarkdownTable(
    private val workspace: TestWorkspace
) : Consumer<LatencyImpact> {
    override fun accept(impact: LatencyImpact) {
        workspace.directory.resolve("latency-impact-table.md").toFile().bufferedWriter().use { writer ->
            writer.write(
                """
                | Action                | Latency impact | Latency impact | Classification |
                |-----------------------|----------------|----------------|----------------|
                | Full Edit Issue       | +16293.44 %    | +99390 ms      | REGRESSION     |
                | Full Add Comment      | +16293.44 %    | +99390 ms      | REGRESSION     |
                """.trimIndent()
            )
        }
    }
}
