package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.apache.commons.lang3.StringUtils.abbreviate
import java.lang.String.format
import java.util.*
import java.util.function.Consumer

class LatencyImpactMarkdownTable(
    private val workspace: TestWorkspace
) : Consumer<LatencyImpact> {

    private val impacts = mutableListOf<LatencyImpact>()

    override fun accept(impact: LatencyImpact) {
        impacts.add(impact)
        workspace.directory.resolve("latency-impact-table.md").toFile().bufferedWriter().use { writer ->
            val formatter = Formatter(writer)
            val format = "| %-21s | %-14s | %-14s | %-14s |\n"
            formatter.format(format, "Action", "Latency impact", "Latency impact", "Classification")
            val dashes21 = "-".repeat(21)
            val dashes14 = "-".repeat(14)
            writer.write("|-$dashes21-|-$dashes14-|-$dashes14-|-$dashes14-|\n")
            impacts.forEach {
                formatter.format(
                    format,
                    abbreviate(it.action.label, 25),
                    format("%+.2f %%", it.relative * 100),
                    format("%+d ms", it.absolute.toMillis()),
                    when {
                        it.regression -> "REGRESSION"
                        it.improvement -> "IMPROVEMENT"
                        else -> "NOISE"
                    }
                )
            }
        }
    }
}
