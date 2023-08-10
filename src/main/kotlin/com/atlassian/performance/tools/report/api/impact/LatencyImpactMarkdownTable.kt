package com.atlassian.performance.tools.report.api.impact

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.impact.ImpactClassification.ImpactType.INCONCLUSIVE
import com.atlassian.performance.tools.report.api.impact.ImpactClassification.ImpactType.NO_IMPACT
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.apache.commons.lang3.StringUtils.abbreviate
import java.lang.String.format
import java.util.*
import java.util.function.Consumer

/**
 * Reports latency impacts in a Markdown table, which is easy to copy-paste into Confluence.
 * Able to report live, during the benchmark execution.
 */
class LatencyImpactMarkdownTable(
    private val workspace: TestWorkspace
) : Consumer<ClassifiedLatencyImpact> {

    private val allImpacts = TreeMap<ActionType<*>, ClassifiedLatencyImpact>(compareBy { it.label })
    private val format = "| %-21s | %-14s | %-10s | %-14s | %-14s |\n"

    override fun accept(newestImpact: ClassifiedLatencyImpact) {
        allImpacts[newestImpact.action] = newestImpact
        workspace.directory.resolve("latency-impact-table.md").toFile().bufferedWriter().use { writer ->
            val formatter = Formatter(writer)
            formatter.format(format, "Action", "Classification", "Confidence", "Latency impact", "Latency impact")
            val dashes21 = "-".repeat(21)
            val dashes14 = "-".repeat(14)
            val dashes10 = "-".repeat(10)
            writer.write("|-$dashes21-|-$dashes14-|-$dashes10-|-$dashes14-|-$dashes14-|\n")
            allImpacts.forEach { (_, impact) ->
                renderRow(impact, formatter)
            }
        }
    }

    private fun renderRow(
        impact: ClassifiedLatencyImpact,
        formatter: Formatter
    ) {
        val action = abbreviate(impact.action.label, 21)
        val classification = impact.classification
        val impactType = classification.impactType
        val typeLabel = when (impactType) {
            NO_IMPACT -> "NO IMPACT"
            else -> impactType.toString()
        }
        if (impactType == INCONCLUSIVE) {
            formatter.format(
                format,
                action,
                typeLabel,
                "N/A",
                "N/A",
                "N/A"
            )
        } else {
            formatter.format(
                format,
                action,
                typeLabel,
                format("%.2f", classification.confidence * 100) + " %",
                format("%+.0f", impact.relativeImpact * 100) + " %",
                format("%+d", impact.absoluteImpact.toMillis()) + " ms"
            )
        }
    }
}
