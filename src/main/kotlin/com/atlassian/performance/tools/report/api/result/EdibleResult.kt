package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.infrastructure.api.metric.SystemMetric
import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.report.api.OutlierTrimming
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Holds post-processed performance results ready for analysis.
 *
 * Logs an error if [failure] is present.
 */
class EdibleResult @Deprecated("Use EdibleResult.Builder instead.") constructor(
    val actionMetrics: List<ActionMetric>,
    val systemMetrics: List<SystemMetric>,
    val nodeDistribution: Map<String, Int>,
    val cohort: String,
    val failure: Exception?
) {
    init {
        if (failure != null) {
            LOGGER.error("$cohort failed", failure)
        }
    }

    val actionStats: InteractionStats by lazy {
        StatsMeter().measure(
            result = this,
            centralTendencyMetric = Mean(),
            dispersionMetric = StandardDeviation(),
            outlierTrimming = OutlierTrimming(
                lowerTrim = 0.01,
                upperTrim = 0.99
            )
        )
    }

    val actionLabels: Set<String> = actionMetrics.map { it.label }.toSet()

    companion object {
        private val LOGGER: Logger = LogManager.getLogger(this::class.java)
    }

    class Builder(private var cohort: String) {
        private var actionMetrics: List<ActionMetric> = emptyList()
        private var systemMetrics: List<SystemMetric> = emptyList()
        private var nodeDistribution: Map<String, Int> = emptyMap()
        private var failure: Exception? = null


        fun actionMetrics(actionMetrics: List<ActionMetric>) = apply { this.actionMetrics = actionMetrics }
        fun systemMetrics(systemMetrics: List<SystemMetric>) = apply { this.systemMetrics = systemMetrics }
        fun nodeDistribution(nodeDistribution: Map<String, Int>) = apply { this.nodeDistribution = nodeDistribution }
        fun failure(failure: Exception) = apply { this.failure = failure }

        @SuppressWarnings
        fun build() = EdibleResult(
            cohort = cohort,
            actionMetrics = actionMetrics,
            systemMetrics = systemMetrics,
            nodeDistribution = nodeDistribution,
            failure = failure
        )
    }
}