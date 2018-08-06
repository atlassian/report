package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.ActionMetric
import com.atlassian.performance.tools.jiraactions.ActionResult
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class ChartBuilder {

    private val bucketSize = Duration.ofMinutes(1)

    fun build(
        metrics: List<ActionMetric>
    ): Chart<Instant> {
        val buckets = aggregate(metrics)
        val slicedBuckets = aggregate(slice(metrics))
        val lines = mutableListOf<ChartLine<Instant>>()
        lines += averageLatencies(buckets, label = "Mean latency")
        lines += averageLatenciesPerLabel(metrics)
        lines += countActions(buckets)
        lines += countEncounteredVirtualUsers(buckets)
        lines += countActiveVirtualUsers(slicedBuckets)
        lines += countAliveVirtualUsers(slicedBuckets)
        lines += getErrorRate(buckets)
        return Chart(lines)
    }

    private fun aggregate(
        metrics: List<ActionMetric>
    ): List<Bucket> {
        return metrics
            .groupBy { it.start.truncatedTo(ChronoUnit.MINUTES) }
            .entries
            .map { Bucket(it.key, it.value) }
            .sorted()
    }

    private fun averageLatenciesPerLabel(
        metrics: List<ActionMetric>
    ): List<ChartLine<Instant>> = metrics
        .map { it.label }
        .toSet()
        .sorted()
        .map { label ->
            averageLatencies(
                aggregate(metrics.filter { metric -> metric.label == label }),
                label,
                hidden = true
            )
        }

    private fun countEncounteredVirtualUsers(
        buckets: List<Bucket>
    ): ChartLine<Instant> {
        val totalEncountered = mutableSetOf<UUID>()
        return ChartLine(
            label = "Encountered Virtual Users",
            yAxisId = "count-axis",
            type = "line",
            hidden = true,
            data = buckets.map { bucket ->
                bucket.toPoint { metrics ->
                    val encountered = metrics
                        .asSequence()
                        .map { it.virtualUser }
                        .toSet()
                    totalEncountered.addAll(encountered)
                    return@toPoint totalEncountered.size.toDouble()
                }
            }
        )
    }

    private fun countAliveVirtualUsers(
        buckets: List<Bucket>
    ): ChartLine<Instant> = ChartLine(
        label = "Alive Virtual Users",
        yAxisId = "count-axis",
        type = "line",
        hidden = true,
        data = buckets.map { bucket ->
            bucket.toPoint { metrics ->
                metrics
                    .map { it.virtualUser }
                    .toSet()
                    .count()
                    .toDouble()
            }
        }
    )

    private fun countActiveVirtualUsers(
        buckets: List<Bucket>
    ): ChartLine<Instant> = ChartLine(
        label = "Active Virtual Users",
        yAxisId = "count-axis",
        type = "line",
        hidden = false,
        data = buckets.map { bucket ->
            bucket.toPoint { metrics ->
                metrics
                    .groupBy { it.virtualUser }
                    .values
                    .flatMap { compact(it) }
                    .map { it.duration }
                    .fold(Duration.ZERO) { total, duration -> total + duration }
                    .toMillis()
                    .toDouble()
                    .div(bucketSize.toMillis())
            }
        }
    )

    /**
     * Slices wide metrics to thinner metrics, each fitting into a bucket.
     */
    private fun slice(
        metrics: List<ActionMetric>
    ): List<ActionMetric> = metrics.flatMap { whole ->
        val slices = mutableListOf<ActionMetric>()
        var sliceStart = whole.start
        val wholeEnd = whole.end
        while (sliceStart < wholeEnd) {
            val sliceEnd = minOf(
                sliceStart.truncatedTo(ChronoUnit.MINUTES) + bucketSize,
                wholeEnd
            )
            slices += whole.copy(
                start = sliceStart,
                duration = Duration.between(sliceStart, sliceEnd)
            )
            sliceStart = sliceEnd
        }
        return@flatMap slices
    }

    /**
     * Compacts overlapping metrics.
     */
    private fun compact(
        metrics: List<ActionMetric>
    ): List<ActionMetric> = metrics
        .sortedBy { it.start }
        .fold(listOf()) { compacted, metric ->
            return@fold when {
                compacted.isEmpty() -> listOf(metric)
                metric.end < compacted.last().end -> compacted
                else -> compacted + metric
            }
        }

    private fun countActions(
        buckets: List<Bucket>
    ): ChartLine<Instant> = ChartLine(
        label = "Actions",
        yAxisId = "count-axis",
        type = "line",
        hidden = false,
        data = buckets.map { bucket ->
            bucket.toPoint { metrics ->
                metrics
                    .count()
                    .toDouble()
            }
        }
    )

    private fun getErrorRate(
        buckets: List<Bucket>
    ): ChartLine<Instant> = ChartLine(
        label = "Error rate",
        yAxisId = "percentage-axis",
        type = "line",
        hidden = false,
        data = buckets.map { bucket ->
            bucket.toPoint { metrics ->
                val errors = metrics
                    .filter { it.result == ActionResult.ERROR }
                    .count()
                    .toDouble()
                val total = metrics
                    .count()
                    .toDouble()
                return@toPoint (errors / total) * 100.0
            }
        }
    )

    private fun averageLatencies(
        buckets: List<Bucket>,
        label: String,
        hidden: Boolean = false
    ): ChartLine<Instant> = ChartLine(
        label = label,
        yAxisId = "duration-axis",
        type = "line",
        hidden = hidden,
        data = buckets.map { bucket ->
            bucket.toPoint { metrics ->
                metrics
                    .map { it.duration.toMillis() }
                    .average()
            }
        }
    )
}

private data class Bucket(
    private val start: Instant,
    private val metrics: List<ActionMetric>
) : Comparable<Bucket> {

    fun toPoint(
        metricsReduction: (List<ActionMetric>) -> Double
    ): Tick = Tick(
        time = start,
        value = metricsReduction(metrics)
    )

    override fun compareTo(
        other: Bucket
    ): Int = compareBy<Bucket> { it.start }.compare(this, other)
}