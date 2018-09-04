package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.jiraactions.api.ActionMetricStatistics
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.OutlierTrimming
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import java.time.Duration

class StatsMeter {

    fun measure(
        result: EdibleResult,
        centralTendencyMetric: UnivariateStatistic,
        dispersionMetric: UnivariateStatistic,
        outlierTrimming: OutlierTrimming
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
        val metrics = result.actionMetrics
        val statistics = ActionMetricStatistics(metrics)
        val centers = mutableMapOf<String, Duration>()
        val dispersions = mutableMapOf<String, Duration>()
        val sampleSizes = mutableMapOf<String, Long>()
        val errors = mutableMapOf<String, Int>()
        for (label in result.actionLabels) {
            val durationData = ActionMetricsReader().read(metrics)[label] ?: DurationData.createEmptyMilliseconds()
            centers[label] = measure(durationData, centralTendencyMetric, outlierTrimming)
            dispersions[label] = measure(durationData, dispersionMetric, outlierTrimming)

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