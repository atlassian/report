package com.atlassian.performance.tools.report.api.impact

import org.apache.commons.math3.distribution.NormalDistribution
import kotlin.math.absoluteValue
import kotlin.math.sqrt

class ImpactClassification(
    val impactType: ImpactType,
    private val favorableOutcomes: Int,
    private val unfavorableOutcomes: Int
) {

    val confidence: Double by lazy {
        val n = favorableOutcomes + unfavorableOutcomes
        val mu = 0.0
        val sigma = 1.0 / sqrt(n.toDouble())
        val samplingDistribution = NormalDistribution(mu, sigma)
        val sampleSum = (favorableOutcomes - unfavorableOutcomes).toDouble()
        val sampleMean = sampleSum / n
        val sampleMeanAbs = sampleMean.absoluteValue
        samplingDistribution.probability(-sampleMeanAbs, sampleMeanAbs)
    }

    enum class ImpactType {
        REGRESSION,
        IMPROVEMENT,
        NO_IMPACT,
        INCONCLUSIVE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImpactClassification

        if (impactType != other.impactType) return false
        if (favorableOutcomes != other.favorableOutcomes) return false
        if (unfavorableOutcomes != other.unfavorableOutcomes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = impactType.hashCode()
        result = 31 * result + favorableOutcomes
        result = 31 * result + unfavorableOutcomes
        return result
    }

    override fun toString(): String {
        return "ImpactType(impactType=$impactType, favorableOutcomes=$favorableOutcomes, unfavorableOutcomes=$unfavorableOutcomes, confidence=$confidence)"
    }
}
