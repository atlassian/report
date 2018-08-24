package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.infrastructure.api.metric.SystemMetric
import com.atlassian.performance.tools.jiraactions.ActionMetric
import com.atlassian.performance.tools.report.api.OutlierTrimming
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

data class EdibleResult(
    val actionMetrics: List<ActionMetric>,
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
}