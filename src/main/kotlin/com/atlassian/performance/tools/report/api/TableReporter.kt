package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.report.MeanAggregator
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.table.AuiTabbedTableFactory
import com.atlassian.performance.tools.report.table.AuiTable
import com.atlassian.performance.tools.report.table.AuiTableFactory
import org.apache.logging.log4j.LogManager
import java.io.File

class TableReporter(
    private val output: File,
    private val labels: List<String>
) {
    private val logger = LogManager.getLogger(this::class.java)

    fun report(
        data: Collection<InteractionStats>
    ) {
        output.ensureParentDirectory().bufferedWriter().use {
            reportHtml(data, it)
        }
        logger.info("Performance results summary available at ${output.toURI()}")
    }

    private fun reportHtml(
        data: Collection<InteractionStats>,
        target: Appendable
    ) {
        val responseTimes = AuiTableFactory(
            headers = listOf("cohort") + labels + "mean",
            rows = data.map { stats ->
                (stats.centers?.mapValues { (_, c) -> c.toMillis().toString() }?.toMutableMap() ?: mutableMapOf())
                    .apply { put("cohort", stats.cohort) }
                    .apply { put("mean", MeanAggregator().aggregateCenters(labels, stats)?.toString() ?: "") }
            }
        ).create()
        val standardDeviation = AuiTableFactory(
            headers = listOf("cohort") + labels,
            rows = data.map { stats ->
                (stats.dispersions?.mapValues { (_, d) -> d.toMillis().toString() }?.toMutableMap() ?: mutableMapOf())
                    .apply { put("cohort", stats.cohort) }
            }
        ).create()
        val requestCount = summingTable(data) { this.sampleSizes }
        val errorCount = summingTable(data) { this.errors }

        val tabbedTable = AuiTabbedTableFactory(
            namedTables = listOf(
                responseTimes to "Response times",
                requestCount to "Request counts",
                errorCount to "Error counts",
                standardDeviation to "Standard deviation"
            )
        ).create().toHtml()

        val report = this::class
            .java
            .getResourceAsStream("aui-table-template.html")
            .bufferedReader()
            .use { it.readText() }
            .replace(
                oldValue = "'<%= table =%>'",
                newValue = tabbedTable
            )
        target.append(report)
    }

    private fun summingTable(
        data: Collection<InteractionStats>,
        metric: InteractionStats.() -> Map<String, Number>?
    ): AuiTable = AuiTableFactory(
        headers = listOf("cohort") + labels + "sum",
        rows = data.map { stats ->
            (stats.metric()?.mapValues { (_, v) -> v.toString() }?.toMutableMap() ?: mutableMapOf())
                .apply { put("sum", labels.map { stats.metric()?.get(it)?.toLong() ?: 0 }.sum().toString()) }
                .apply { this["cohort"] = stats.cohort }
        }
    ).create()
}