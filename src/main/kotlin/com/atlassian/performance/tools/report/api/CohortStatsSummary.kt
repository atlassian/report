package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.io.api.ensureParentDirectory
import com.atlassian.performance.tools.report.MeanAggregator
import com.atlassian.performance.tools.report.api.result.Stats
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.logging.log4j.LogManager
import java.io.File
import java.time.Duration

/**
 * Summarizes per cohort statistical results.
 */
class CohortStatsSummary(
    private val output: File,
    private val labels: List<String>
) {
    private val logger = LogManager.getLogger(this::class.java)

    fun report(
        data: Collection<Stats>
    ) {
        output.ensureParentDirectory().bufferedWriter().use {
            reportCsv(data, it)
        }
        logger.info("Performance results summary available at ${output.toURI()}")
    }

    private fun reportCsv(
        data: Collection<Stats>,
        target: Appendable
    ) {
        val headers = arrayOf("cohort", "metric") + labels + "AGGREGATE"
        val format = CSVFormat.DEFAULT.withHeader(*headers).withRecordSeparator('\n')
        val printer = CSVPrinter(target, format)

        printer.printRecords(data.map { stats ->
            listOf(
                stats.cohort,
                "response time average"
            ) + labels.map { stats.locations[it] }.map { it?.serialize() } + MeanAggregator().aggregateCenters(labels, stats)
        })
        printer.printRecords(data.map { stats ->
            listOf(stats.cohort, "request count") + labels.map { stats.sampleSizes[it] }
        })
        printer.printRecords(data.map { stats ->
            listOf(
                stats.cohort,
                "response time standard deviation"
            ) + labels.map { stats.dispersions[it] }.map { it?.serialize() }
        })
        printer.printRecords(data.map { stats ->
            listOf(stats.cohort, "error count") + labels.map { stats.errors[it] }
        })
        printer.flush()
    }

    private fun Duration.serialize(): Long = this.toMillis()
}