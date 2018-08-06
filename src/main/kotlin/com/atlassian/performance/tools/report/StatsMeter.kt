package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.ActionMetricStatistics
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import java.time.Duration

class StatsMeter {

    fun measure(
        result: EdibleResult
    ): InteractionStats {
        if (result.failure != null) {
            return InteractionStats(
                cohort = result.cohort,
                sampleSizes = null,
                centers = null,
                dispersions = null,
                errors = null
            )
        }
        val metrics = result.criticalActionMetrics
        val statistics = ActionMetricStatistics(metrics)
        val centers = mutableMapOf<String, Duration>()
        val dispersions = mutableMapOf<String, Duration>()
        val sampleSizes = mutableMapOf<String, Long>()
        val errors = mutableMapOf<String, Int>()
        for ((action, criteria) in result.criteria.actionCriteria) {
            val label = action.label
            val durationData = ActionMetricsReader().read(metrics)[label] ?: DurationData.createEmptyMilliseconds()
            centers[label] = measure(durationData, criteria.centerCriteria.centralTendencyMetric, criteria.outlierTrimming)
            dispersions[label] = measure(durationData, criteria.dispersionCriteria.dispersionMetric, criteria.outlierTrimming)

            sampleSizes[label] = statistics.sampleSize.getOrDefault(label, 0).toLong()
            errors[label] = statistics.errors.getOrDefault(label, 0)
        }
        return InteractionStats(result.cohort, sampleSizes, centers, dispersions, errors)
    }

    fun measure(
        data: DurationData,
        metric: UnivariateStatistic,
        outlierTrimming: OutlierTrimming
    ): Duration {
        val measurement = outlierTrimming.measureWithoutOutliers(
            data = data.stats,
            metric = metric
        )
        return data.durationMapping(measurement)
    }
}