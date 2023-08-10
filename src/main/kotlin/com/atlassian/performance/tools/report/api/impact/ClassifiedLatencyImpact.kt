package com.atlassian.performance.tools.report.api.impact

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import java.time.Duration

/**
 * @param relativeImpact median of [LatencyImpact.relativeDiff]
 * @param absoluteImpact median of [LatencyImpact.absoluteDiff]
 */
class ClassifiedLatencyImpact(
    val action: ActionType<*>,
    val classification: ImpactClassification,
    val relativeImpact: Double,
    val absoluteImpact: Duration
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassifiedLatencyImpact

        if (action != other.action) return false
        if (classification != other.classification) return false
        if (relativeImpact != other.relativeImpact) return false
        if (absoluteImpact != other.absoluteImpact) return false

        return true
    }

    override fun hashCode(): Int {
        var result = action.hashCode()
        result = 31 * result + classification.hashCode()
        result = 31 * result + relativeImpact.hashCode()
        result = 31 * result + absoluteImpact.hashCode()
        return result
    }

    override fun toString(): String {
        return "ActionImpact(action=${action.label}, classification=$classification, relativeImpact=$relativeImpact, absoluteImpact=$absoluteImpact)"
    }
}
