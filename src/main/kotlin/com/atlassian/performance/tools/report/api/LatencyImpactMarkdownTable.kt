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
            val format = "| %-21s | %-14s | %-14s | %-14s | %-10s |\n"
            formatter.format(format, "Action", "Latency impact", "Latency impact", "Classification", "Confidence")
            val dashes21 = "-".repeat(21)
            val dashes14 = "-".repeat(14)
            val dashes10 = "-".repeat(10)
            writer.write("|-$dashes21-|-$dashes14-|-$dashes14-|-$dashes14-|-$dashes10-|\n")
            impacts.forEach {
                val action = abbreviate(it.action.label, 25)
                val relativeImpact = format("%+.2f %%", it.relativeDiff * 100)
                val absoluteImpact = format("%+d ms", it.absoluteDiff.toMillis())
                val classification = when {
                    it.regression -> "REGRESSION"
                    it.improvement -> "IMPROVEMENT"
                    else -> "NO IMPACT"
                }
                val confidence = if (it.irrelevant) "-" else "68 %"
                formatter.format(format, action, relativeImpact, absoluteImpact, classification, confidence)
            }
        }
    }
}
