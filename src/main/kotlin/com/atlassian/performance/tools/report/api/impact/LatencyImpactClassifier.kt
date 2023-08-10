package com.atlassian.performance.tools.report.api.impact

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.impact.ImpactClassification.ImpactType.*
import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import org.apache.commons.math3.stat.descriptive.rank.Median
import java.time.Duration
import java.util.function.Consumer

/**
 * Turns a bunch of [LatencyImpact]s to [ClassifiedLatencyImpact]s.
 */
class LatencyImpactClassifier private constructor(
    private val handlers: List<Consumer<ClassifiedLatencyImpact>>
) : Consumer<LatencyImpact> {

    private val unclassifiedImpacts = mutableListOf<LatencyImpact>()
    private val classifiedImpacts = mutableMapOf<ActionType<*>, ClassifiedLatencyImpact>()

    override fun accept(newestImpact: LatencyImpact) {
        unclassifiedImpacts.add(newestImpact)
        unclassifiedImpacts.groupBy { it.action }.map { (actionGroup, impacts) ->
            val element = element(actionGroup, impacts)
            classifiedImpacts[actionGroup] = element
            handlers.forEach { it.accept(element) }
        }
    }

    private fun element(
        actionGroup: ActionType<*>,
        impacts: List<LatencyImpact>
    ): ClassifiedLatencyImpact {
        val classification = classify(impacts)
        return ClassifiedLatencyImpact(
            actionGroup,
            classification,
            relativeImpact(impacts),
            absoluteImpact(impacts)
        )
    }

    private fun relativeImpact(impacts: List<LatencyImpact>): Double {
        return Median()
            .also { median -> median.data = impacts.map { it.relativeDiff }.toDoubleArray() }
            .evaluate()
    }

    private fun absoluteImpact(impacts: List<LatencyImpact>): Duration {
        val median = Median()
            .also { median -> median.data = impacts.map { it.absoluteDiff.toMillis().toDouble() }.toDoubleArray() }
            .evaluate()
        return Duration.ofMillis(median.toLong())
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

    fun classify(): List<ClassifiedLatencyImpact> {
        return ArrayList(classifiedImpacts.values)
    }

    class Builder {
        private val handlers: MutableList<Consumer<ClassifiedLatencyImpact>> = mutableListOf()

        fun handleLatencyImpact(handler: Consumer<ClassifiedLatencyImpact>) = apply { this@Builder.handlers.add(handler) }

        fun build(): LatencyImpactClassifier = LatencyImpactClassifier(handlers)
    }

}
