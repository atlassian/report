package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.result.PrintableInteractionStats
import com.atlassian.performance.tools.report.table.AuiTabbedTableFactory
import com.atlassian.performance.tools.report.table.AuiTableFactory
import com.atlassian.performance.tools.report.table.NamedAuiTable
import org.apache.logging.log4j.LogManager
import java.io.File

class CohortsSummaryTable(
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
        val printableStats = data.map { PrintableInteractionStats(it, labels) }

        val responseTimes = AuiTableFactory(
            headers = listOf("cohort") + labels + "mean",
            rows = printableStats.map { stats ->
                stats.centers.toMutableMap()
                    .apply { put("cohort", stats.cohort) }
                    .apply { put("mean", stats.mean)}
            }
        ).create()
        val standardDeviation = AuiTableFactory(
            headers = listOf("cohort") + labels,
            rows = printableStats.map { stats ->
                stats.dispersions.toMutableMap()
                    .apply { put("cohort", stats.cohort) }
            }
        ).create()
        val requestCount = AuiTableFactory(
            headers = listOf("cohort") + labels + "sum",
            rows = printableStats.map {stats ->
                stats.sampleSizes.toMutableMap()
                    .apply { put("cohort", stats.cohort) }
                    .apply { put("sum", stats.requestCount) }
            }
        ).create()
        val errorCount = AuiTableFactory(
            headers = listOf("cohort") + labels + "sum",
            rows = printableStats.map { stats ->
                stats.errors.toMutableMap()
                    .apply { put("cohort", stats.cohort) }
                    .apply { put("sum", stats.errorCount) }
            }
        ).create()

        val tabbedTable = AuiTabbedTableFactory(
            namedTables = listOf(
                NamedAuiTable(table = responseTimes, name = "Response times"),
                NamedAuiTable(table = requestCount, name = "Request counts"),
                NamedAuiTable(table = errorCount, name = "Error counts"),
                NamedAuiTable(table = standardDeviation, name = "Standard deviation")
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

}