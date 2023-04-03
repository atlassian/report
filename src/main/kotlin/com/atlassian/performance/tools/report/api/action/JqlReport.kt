package com.atlassian.performance.tools.report.api.action

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.jiraactions.api.observation.SearchJqlObservation
import com.atlassian.performance.tools.report.api.result.DurationData
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.nio.file.Path

class JqlReport private constructor(
    private val jqlTypes: List<ActionType<SearchJqlObservation>>
) {
    fun report(
        allMetrics: List<ActionMetric>,
        target: Path
    ) {
        val jqlObservations: List<JqlActionMetric> = allMetrics
            .filter { metric -> metric.observation != null }
            .mapNotNull { metric ->
                jqlTypes
                    .singleOrNull { type -> type.label == metric.label }
                    ?.deserialize(metric.observation!!)
                    ?.let { observation -> JqlActionMetric(metric, observation) }
            }
        entries(jqlObservations, target.resolve("search-jql-entries.csv"))
        stats(jqlObservations, target.resolve("search-jql-stats.csv"))
    }

    private class JqlActionMetric(
        val metric: ActionMetric,
        val observation: SearchJqlObservation
    )

    private fun entries(
        jqlMetrics: List<JqlActionMetric>,
        target: Path
    ) {
        val headers = arrayOf("jql", "result", "issues", "totalResults", "duration")
        val format = CSVFormat.DEFAULT.withHeader(*headers)
        val printer = CSVPrinter(target.toFile().ensureParentDirectory().bufferedWriter(), format)

        printer.printRecords(jqlMetrics.map(::toCsvEntries))
        printer.flush()
    }

    private fun toCsvEntries(jqlMetric: JqlActionMetric): List<*> {
        return with(jqlMetric) {
            listOf(
                observation.jql,
                metric.result,
                observation.issues,
                observation.totalResults,
                metric.duration.toMillis()
            )
        }
    }

    private fun stats(
        jqlMetrics: List<JqlActionMetric>,
        target: Path
    ) {
        val stats = jqlMetrics
            .groupBy({ it.observation.jql }, { it })
            .map { aggregate(it.key, it.value) }

        val headers = arrayOf("jql", "n", "latency", "minTotalResults", "maxTotalResults")
        val format = CSVFormat.DEFAULT.withHeader(*headers)
        val printer = CSVPrinter(target.toFile().bufferedWriter(), format)

        printer.printRecords(stats.map {
            listOf(
                it.jql,
                it.n,
                it.latency,
                it.minTotalResults,
                it.maxTotalResults
            )
        })

        printer.flush()
    }

    private fun aggregate(
        jql: String,
        metrics: List<JqlActionMetric>
    ): SearchJqlStats {
        val duration = metrics
            .map { it.metric }
            .fold(DurationData.createEmptyNanoseconds(), ::accumulateMetric)
        val minTotalResults = metrics.map { it.observation.totalResults }.min()!!
        val maxTotalResults = metrics.map { it.observation.totalResults }.max()!!
        val averageLatency = duration.durationMapping(duration.stats.mean)

        return SearchJqlStats(
            jql = jql,
            n = duration.stats.n,
            latency = averageLatency,
            minTotalResults = minTotalResults,
            maxTotalResults = maxTotalResults
        )
    }

    private fun accumulateMetric(
        data: DurationData,
        metric: ActionMetric
    ): DurationData {
        data.stats.addValue(metric.duration.toNanos().toDouble())
        return data
    }

    class Builder {
        private var jqlTypes: List<ActionType<SearchJqlObservation>> = listOf(
            SEARCH_WITH_JQL,
            SEARCH_JQL_SIMPLE,
            SEARCH_JQL_CHANGELOG,
            SEARCH_WITH_JQL_WILDCARD
        )

        fun jqlTypes(jqlTypes: List<ActionType<SearchJqlObservation>>) = apply { this.jqlTypes = jqlTypes }

        fun build() = JqlReport(jqlTypes)
    }
}
