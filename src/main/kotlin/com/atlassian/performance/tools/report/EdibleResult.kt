package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.infrastructure.metric.SystemMetric
import com.atlassian.performance.tools.jiraactions.ActionMetric
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

data class EdibleResult(
    val criteria: PerformanceCriteria,
    val allActionMetrics: List<ActionMetric>,
    val systemMetrics: List<SystemMetric>,
    val nodeDistribution: Map<String, Int>,
    val cohort: String,
    val failure: Exception?
) {
    init {
        if (failure != null) {
            LOGGER.debug("$cohort failed", failure)
        }
    }

    val actionStats: InteractionStats by lazy {
        StatsMeter().measure(this)
    }

    val criticalActionLabels = criteria.actionCriteria.keys.map { it.label }

    val criticalActionMetrics: List<ActionMetric> by lazy {
        allActionMetrics.filter { it.label in criticalActionLabels }
    }

    companion object {
        private val LOGGER: Logger = LogManager.getLogger(this::class.java)
    }
}