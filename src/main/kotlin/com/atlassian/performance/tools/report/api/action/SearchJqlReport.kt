package com.atlassian.performance.tools.report.api.action

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.SEARCH_WITH_JQL
import com.atlassian.performance.tools.jiraactions.api.observation.SearchJqlObservation
import com.atlassian.performance.tools.report.api.result.DurationData
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.nio.file.Path

class SearchJqlReport(
    allMetrics: List<ActionMetric>
) {
    private val metrics = allMetrics.filter { it.label == SEARCH_WITH_JQL.label }

    fun report(
        target: Path
    ) {
        entries(target.resolve("search-jql-entries.csv"))
        stats(target.resolve("search-jql-stats.csv"))
    }

    private fun entries(
        target: Path
    ) {
        val headers = arrayOf("jql", "result", "issues", "totalResults", "duration")
        val format = CSVFormat.DEFAULT.withHeader(*headers)
        val printer = CSVPrinter(target.toFile().ensureParentDirectory().bufferedWriter(), format)

        printer.printRecords(metrics.map(::toCsvEntries))
        printer.flush()
    }

    private fun toCsvEntries(
        metric: ActionMetric
    ): List<*> {
        val observation: SearchJqlObservation? = metric.observation?.let { SearchJqlObservation(it) }

        return listOf(
            observation?.jql ?: "",
            metric.result,
            observation?.issues ?: "",
            observation?.totalResults ?: "",
            metric.duration.toMillis()
        )
    }

    private fun stats(
        target: Path
    ) {
        val stats = metrics.map { it to it.observation?.let { SearchJqlObservation(it) } }
            .filter { it.second != null }
            .groupBy({ it.second!!.jql }, { it })
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
        metrics: List<Pair<ActionMetric, SearchJqlObservation?>>
    ): SearchJqlStats {
        val duration = metrics.map { it.first }
            .fold(DurationData.createEmptyNanoseconds(), ::accumulateMetric)
        val minTotalResults = metrics.map { it.second!!.totalResults }.min()!!
        val maxTotalResults = metrics.map { it.second!!.totalResults }.max()!!
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
}