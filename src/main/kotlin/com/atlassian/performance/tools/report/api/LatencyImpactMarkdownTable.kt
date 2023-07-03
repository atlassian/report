package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.LatencyImpactMarkdownTable.ImpactClassification.Label.*
import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.apache.commons.lang3.StringUtils.abbreviate
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.stat.descriptive.rank.Median
import java.lang.String.format
import java.util.*
import java.util.function.Consumer
import kotlin.math.absoluteValue
import kotlin.math.sqrt

class LatencyImpactMarkdownTable(
    private val workspace: TestWorkspace
) : Consumer<LatencyImpact> {

    private val allImpacts = mutableListOf<LatencyImpact>()
    private val format = "| %-21s | %-14s | %-10s | %-14s | %-14s |\n"

    override fun accept(newestImpact: LatencyImpact) {
        allImpacts.add(newestImpact)
        workspace.directory.resolve("latency-impact-table.md").toFile().bufferedWriter().use { writer ->
            val formatter = Formatter(writer)
            formatter.format(format, "Action", "Classification", "Confidence", "Latency impact", "Latency impact")
            val dashes21 = "-".repeat(21)
            val dashes14 = "-".repeat(14)
            val dashes10 = "-".repeat(10)
            writer.write("|-$dashes21-|-$dashes14-|-$dashes10-|-$dashes14-|-$dashes14-|\n")
            allImpacts.groupBy { it.action }.forEach { (actionGroup, impacts) ->
                renderRow(actionGroup, impacts, formatter)
            }
        }
    }

    private fun renderRow(
        actionGroup: ActionType<*>,
        impacts: List<LatencyImpact>,
        formatter: Formatter
    ) {
        val action = abbreviate(actionGroup.label, 21)
        val classification = classify(impacts)
        if (classification.label == INCONCLUSIVE) {
            formatter.format(
                format,
                action,
                classification,
                "-",
                "-",
                "-"
            )
        } else {
            formatter.format(
                format,
                action,
                classification,
                format("%.2f", classification.confidence() * 100) + " %",
                relativeImpact(impacts),
                absoluteImpact(impacts)
            )
        }
    }

    private fun relativeImpact(impacts: List<LatencyImpact>): String {
        val median = Median()
            .also { median -> median.data = impacts.map { it.relativeDiff }.toDoubleArray() }
            .evaluate()
        return format("%+.0f", median * 100) + " %"
    }

    private fun absoluteImpact(impacts: List<LatencyImpact>): String {
        val median = Median()
            .also { median -> median.data = impacts.map { it.absoluteDiff.toMillis().toDouble() }.toDoubleArray() }
            .evaluate()
        return format("%+.0f", median) + " ms"
    }

    private fun classify(impacts: List<LatencyImpact>): ImpactClassification {
        val regressions = impacts.count { it.regression }
        val improvements = impacts.count { it.improvement }
        val irrelevants = impacts.count { it.irrelevant }
        return if (regressions > (improvements + irrelevants)) {
            ImpactClassification(REGRESSION, regressions, improvements + irrelevants)
        } else if (improvements > (regressions + irrelevants)) {
            ImpactClassification(IMPROVEMENT, improvements, regressions + irrelevants)
        } else if (irrelevants > (improvements + regressions)) {
            ImpactClassification(NO_IMPACT, irrelevants, improvements + regressions)
        } else {
            ImpactClassification(INCONCLUSIVE, 0, 0)
        }
    }

    private class ImpactClassification(
        val label: Label,
        private val favorableOutcomes: Int,
        private val unfavorableOutcomes: Int
    ) {
        enum class Label(
            val display: String
        ) {
            REGRESSION("REGRESSION"),
            IMPROVEMENT("IMPROVEMENT"),
            NO_IMPACT("NO IMPACT"),
            INCONCLUSIVE("INCONCLUSIVE")
        }

        fun confidence(): Double {
            val n = favorableOutcomes + unfavorableOutcomes
            val mu = 0.0
            val sigma = 1.0 / sqrt(n.toDouble())
            val samplingDistribution = NormalDistribution(mu, sigma)
            val sampleSum = (favorableOutcomes - unfavorableOutcomes).toDouble()
            val sampleMean = sampleSum / n
            val sampleMeanAbs = sampleMean.absoluteValue
            return samplingDistribution.probability(-sampleMeanAbs, sampleMeanAbs)
        }

        override fun toString() = label.display
    }

}
