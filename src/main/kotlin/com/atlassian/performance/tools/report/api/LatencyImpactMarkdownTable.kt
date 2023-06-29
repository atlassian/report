package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.apache.commons.lang3.StringUtils.abbreviate
import java.util.*
import java.util.function.Consumer

class LatencyImpactMarkdownTable(
    private val workspace: TestWorkspace
) : Consumer<LatencyImpact> {

    private val impacts = mutableListOf<LatencyImpact>()

    override fun accept(impact: LatencyImpact) {
        impacts.add(impact)
        workspace.directory.resolve("latency-impact-table.md").toFile().bufferedWriter().use { writer ->
            writer.write("| Action                | Latency impact | Latency impact | Classification |\n")
            writer.write("|-----------------------|----------------|----------------|----------------|\n")
            val formatter = Formatter(writer)
            impacts.forEach {
                formatter.format(
                    "| %-21s | %-14s | %-14s | %-14s |\n",
                    abbreviate(it.action.label, 25),
                    String.format("%+.2f %%", it.relative * 100),
                    String.format("%+d ms", it.absolute.toMillis()),
                    if (it.noise) {
                        "NOISE"
                    } else {
                        if (it.regression) {
                            "REGRESSION"
                        } else {
                            "IMPROVEMENT"
                        }
                    }
                )
            }
        }
    }
}
