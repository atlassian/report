package com.atlassian.performance.tools.report.action

import com.atlassian.performance.tools.io.ensureParentDirectory
import com.atlassian.performance.tools.jiraactions.ActionMetric
import com.atlassian.performance.tools.jiraactions.SEARCH_WITH_JQL
import com.atlassian.performance.tools.jiraactions.SearchJqlObservation
import com.atlassian.performance.tools.report.DurationData
import com.atlassian.performance.tools.report.OutlierTrimming
import com.atlassian.performance.tools.report.PerformanceCriteria
import com.atlassian.performance.tools.report.StatsMeter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.apache.logging.log4j.LogManager
import java.nio.file.Path

class SearchJqlReport(
    criteria: PerformanceCriteria,
    allMetrics: List<ActionMetric>
) {
    private val logger = LogManager.getLogger(this::class.java)
    private val metrics = allMetrics.filter { it.label == SEARCH_WITH_JQL.label }
    private val outlierTrimming = criteria.actionCriteria[SEARCH_WITH_JQL]?.outlierTrimming
    private val statsMeter = StatsMeter()

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
        if (outlierTrimming == null) {
            logger.debug("No criteria for $SEARCH_WITH_JQL. SearchJqlReport stats won't be available.")
            return
        }

        val stats = metrics.map { it to it.observation?.let { SearchJqlObservation(it) } }
            .filter { it.second != null }
            .groupBy({ it.second!!.jql }, { it })
            .map { aggregate(it.key, it.value, outlierTrimming) }

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
        metrics: List<Pair<ActionMetric, SearchJqlObservation?>>,
        outlierTrimming: OutlierTrimming
    ): SearchJqlStats {
        val duration = metrics.map { it.first }
            .fold(DurationData.createEmptyNanoseconds(), ::accumulateMetric)
        val minTotalResults = metrics.map { it.second!!.totalResults }.min()!!
        val maxTotalResults = metrics.map { it.second!!.totalResults }.max()!!
        val averageLatency = statsMeter.measure(duration, StandardDeviation(), outlierTrimming)

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