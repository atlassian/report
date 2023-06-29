package com.atlassian.performance.tools.report.result

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionMetricStatistics
import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.OutlierTrimming
import com.atlassian.performance.tools.report.api.result.DurationData
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.api.result.Stats
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import java.time.Duration

internal class InternalStatsMeter {

    private companion object {
        private val DEFAULT_TRIMMING: OutlierTrimming = OutlierTrimming(0.0, 1.0)
    }

    internal fun measurePerformance(
        result: EdibleResult,
        centralTendencyMetric: UnivariateStatistic,
        dispersionMetric: UnivariateStatistic,
        trimmingPerType: Map<ActionType<*>, OutlierTrimming>
    ): Stats {
        if (result.failure != null) {
            return PerformanceStats(
                cohort = result.cohort,
                sampleSizes = emptyMap(),
                locations = emptyMap(),
                dispersions = emptyMap(),
                errors = emptyMap()
            )
        }
        val metrics = result.actionMetrics
        val statistics = ActionMetricStatistics(metrics)
        val outliersPerLabel = trimmingPerType.mapKeys { it.key.label }
        val centers = calculate(metrics, centralTendencyMetric, outliersPerLabel)
        val dispersions = calculate(metrics, dispersionMetric, outliersPerLabel)
        val sampleSizes = mutableMapOf<String, Long>()
        val errors = mutableMapOf<String, Int>()
        for (label in result.actionLabels) {
            sampleSizes[label] = statistics.sampleSize.getOrDefault(label, 0).toLong()
            errors[label] = statistics.errors.getOrDefault(label, 0)
        }
        return PerformanceStats(result.cohort, sampleSizes, centers, dispersions, errors)
    }

    internal fun calculate(
        metrics: List<ActionMetric>,
        metric: UnivariateStatistic,
        outlierTrimming: OutlierTrimming
    ): Map<String, Duration> {
        val labels = metrics.map { it.label }.toSet()
        return labels
            .asSequence()
            .map { label -> label to calculate(label, metrics, metric, outlierTrimming) }
            .toMap()
    }

    private fun calculate(
        label: String,
        metrics: List<ActionMetric>,
        metric: UnivariateStatistic,
        outlierTrimming: OutlierTrimming?
    ): Duration {
        val durationData = ActionMetricsReader().read(metrics)[label] ?: DurationData.createEmptyMilliseconds()
        return measureWithoutOutliers(durationData, metric, outlierTrimming)
    }

    private fun calculate(
        metrics: List<ActionMetric>,
        metric: UnivariateStatistic,
        trimmingPerType: Map<String, OutlierTrimming>
    ): Map<String, Duration> {
        val labels = metrics.map { it.label }.toSet()
        return labels
            .asSequence()
            .map { label -> label to calculate(label, metrics, metric, trimmingPerType[label]) }
            .toMap()
    }

    private fun measureWithoutOutliers(
        data: DurationData,
        metric: UnivariateStatistic,
        outlierTrimming: OutlierTrimming?
    ): Duration {
        val trimmer = outlierTrimming ?: DEFAULT_TRIMMING
        val measurement = trimmer.measureWithoutOutliers(
            data = data.stats,
            metric = metric
        )
        return data.durationMapping(measurement)
    }
}
