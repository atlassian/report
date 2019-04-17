package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionMetricStatistics
import com.atlassian.performance.tools.report.ActionMetricsReader
import com.atlassian.performance.tools.report.api.OutlierTrimming
import com.atlassian.performance.tools.report.result.PerformanceStats
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import java.time.Duration

class StatsMeter {

    fun measurePerformance(
        result: EdibleResult,
        centralTendencyMetric: UnivariateStatistic,
        dispersionMetric: UnivariateStatistic,
        outlierTrimming: OutlierTrimming
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
        val centers = calculate(metrics, centralTendencyMetric, outlierTrimming)
        val dispersions = calculate(metrics, dispersionMetric, outlierTrimming)
        val sampleSizes = mutableMapOf<String, Long>()
        val errors = mutableMapOf<String, Int>()
        for (label in result.actionLabels) {
            sampleSizes[label] = statistics.sampleSize.getOrDefault(label, 0).toLong()
            errors[label] = statistics.errors.getOrDefault(label, 0)
        }
        return PerformanceStats(result.cohort, sampleSizes, centers, dispersions, errors)
    }

    @Deprecated(
        message = "Use measurePerformance instead.",
        replaceWith = ReplaceWith(
            expression = "measurePerformance(result, centralTendencyMetric, dispersionMetric, outlierTrimming)"
        )
    )
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
        val centers = calculate(metrics, centralTendencyMetric, outlierTrimming)
        val dispersions = calculate(metrics, dispersionMetric, outlierTrimming)
        val sampleSizes = mutableMapOf<String, Long>()
        val errors = mutableMapOf<String, Int>()
        for (label in result.actionLabels) {
            sampleSizes[label] = statistics.sampleSize.getOrDefault(label, 0).toLong()
            errors[label] = statistics.errors.getOrDefault(label, 0)
        }
        return InteractionStats(result.cohort, sampleSizes, centers, dispersions, errors)
    }

    /**
     * Calculates `metric` for the list of `metrics`. Failed actions are not included in the result.
     *
     * @return Map of results for each label
     * @since 2.4.0
     */
    fun calculate(
        metrics: List<ActionMetric>,
        metric: UnivariateStatistic,
        outlierTrimming: OutlierTrimming
    ): Map<String, Duration> {
        val labels = metrics.map { it.label }.toSet()
        return labels
            .asSequence()
            .map { label -> calculate(label, metrics, metric, outlierTrimming) }
            .toMap()
    }

    private fun calculate(
        label: String,
        metrics: List<ActionMetric>,
        metric: UnivariateStatistic,
        outlierTrimming: OutlierTrimming
    ): Pair<String, Duration> {
        val durationData = ActionMetricsReader().read(metrics)[label] ?: DurationData.createEmptyMilliseconds()
        val results = measure(durationData, metric, outlierTrimming)
        return label to results
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