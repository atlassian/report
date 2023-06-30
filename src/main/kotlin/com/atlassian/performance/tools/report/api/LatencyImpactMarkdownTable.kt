package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.apache.commons.lang3.StringUtils.abbreviate
import org.apache.commons.math3.distribution.NormalDistribution
import java.lang.String.format
import java.time.Duration
import java.util.*
import java.util.function.Consumer
import kotlin.math.absoluteValue
import kotlin.math.sqrt

class LatencyImpactMarkdownTable(
    private val workspace: TestWorkspace
) : Consumer<LatencyImpact> {

    private val allImpacts = mutableListOf<LatencyImpact>()

    override fun accept(newestImpact: LatencyImpact) {
        allImpacts.add(newestImpact)
        workspace.directory.resolve("latency-impact-table.md").toFile().bufferedWriter().use { writer ->
            val formatter = Formatter(writer)
            val format = "| %-21s | %-14s | %-14s | %-14s | %-10s |\n"
            formatter.format(format, "Action", "Latency impact", "Latency impact", "Classification", "Confidence")
            val dashes21 = "-".repeat(21)
            val dashes14 = "-".repeat(14)
            val dashes10 = "-".repeat(10)
            writer.write("|-$dashes21-|-$dashes14-|-$dashes14-|-$dashes14-|-$dashes10-|\n")
            allImpacts.groupBy { it.action }.forEach { (actionGroup, impacts) ->
                val regressions = impacts.count { it.regression }
                val improvements = impacts.count { it.improvement }
                val irrelevants = impacts.count { it.irrelevant }
                val action = abbreviate(actionGroup.label, 25)
                val classification = classify(regressions, improvements, irrelevants)
                val confidence = format("%.2f %%", classification.confidence() * 100)
                formatter.format(
                    format,
                    action,
                    relativeImpact(impacts),
                    absoluteImpact(impacts),
                    classification.label,
                    confidence
                )
            }
        }
    }

    private fun relativeImpact(impacts: List<LatencyImpact>): String {
        val diffs = impacts.map { it.relativeDiff }.toSet()
        if (diffs.size == 1) {
            return relativeImpact(diffs.single())
        }
        val min = relativeImpact(diffs.min()!!)
        val max = relativeImpact(diffs.max()!!)
        return "$min to $max"
    }

    private fun relativeImpact(diff: Double) = format("%+.0f %%", diff * 100)

    private fun absoluteImpact(impacts: List<LatencyImpact>): String {
        val diffs = impacts.map { it.absoluteDiff }.toSet()
        if (diffs.size == 1) {
            return absoluteImpact(diffs.single()) + " ms"
        }
        val min = absoluteImpact(diffs.min()!!)
        val max = absoluteImpact(diffs.max()!!) + " ms"
        return "$min to $max"
    }

    private fun absoluteImpact(diff: Duration) = format("%+d", diff.toMillis())

    private fun classify(regressions: Int, improvements: Int, irrelevants: Int): ImpactClassification {
        return if (regressions > (improvements + irrelevants)) {
            ImpactClassification("REGRESSION", regressions, improvements + irrelevants)
        } else if (improvements > (regressions + irrelevants)) {
            ImpactClassification("IMPROVEMENT", improvements, regressions + irrelevants)
        } else if (irrelevants > (improvements + regressions)) {
            ImpactClassification("NO IMPACT", irrelevants, improvements + regressions)
        } else {
            ImpactClassification("INCONCLUSIVE", 0, improvements + regressions + irrelevants)
        }
    }

    private class ImpactClassification(
        val label: String,
        private val favorableOutcomes: Int,
        private val unfavorableOutcomes: Int
    ) {
        fun confidence(): Double {
            val n = favorableOutcomes + unfavorableOutcomes
            val mu = 0.0
            val sigma = 1.0 / sqrt(n.toDouble())
            val samplingDistribution = NormalDistribution(mu, sigma)
            val sampleSum = (favorableOutcomes - unfavorableOutcomes).toDouble()
            val sampleMean = sampleSum / n
            val rightEdge = samplingDistribution.cumulativeProbability(sampleMean)
            val leftEdge = samplingDistribution.cumulativeProbability(-sampleMean)
            return (rightEdge - leftEdge).absoluteValue
        }
    }
}
