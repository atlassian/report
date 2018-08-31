package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.io.ensureParentDirectory
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.Sample
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.logging.log4j.LogManager
import java.io.File
import java.time.Duration

/**
 * Summarizes per cohort statistical results.
 */
class DataReporter(
    private val output: File,
    private val labels: List<String>
) {
    private val logger = LogManager.getLogger(this::class.java)

    fun report(
        data: Collection<InteractionStats>
    ) {
        output.ensureParentDirectory().bufferedWriter().use {
            reportCsv(data, it)
        }
        logger.info("Performance results summary available at ${output.toURI()}")
    }

    private fun reportCsv(
        data: Collection<InteractionStats>,
        target: Appendable
    ) {
        val headers = arrayOf("cohort", "metric") + labels + "AGGREGATE"
        val format = CSVFormat.DEFAULT.withHeader(*headers).withRecordSeparator('\n')
        val printer = CSVPrinter(target, format)

        printer.printRecords(data.map { stats ->
            listOf(
                stats.cohort,
                "response time average"
            ) + labels.map { stats.centers?.get(it) }.map { it?.serialize() } + aggregateCenters(stats)
        })
        printer.printRecords(data.map { stats ->
            listOf(stats.cohort, "request count") + labels.map { stats.sampleSizes?.get(it) }
        })
        printer.printRecords(data.map { stats ->
            listOf(
                stats.cohort,
                "response time standard deviation"
            ) + labels.map { stats.dispersions?.get(it) }.map { it?.serialize() }
        })
        printer.printRecords(data.map { stats ->
            listOf(stats.cohort, "error count") + labels.map { stats.errors?.get(it) }
        })
        printer.flush()
    }

    private fun Duration.serialize(): Long = this.toMillis()

    private fun aggregateCenters(stats: InteractionStats): Long? {
        val aggregate = Mean()
        val samples = labels.mapNotNull { sampleFor(it, stats) }
        val centers = samples.map { it.center }.toDoubleArray()
        val sampleSizes = samples.map { it.sampleSize }.map { it.toDouble() }.toDoubleArray()

        return when {
            sampleSizes.all { it == 0.0 } -> null
            else -> aggregate.evaluate(centers, sampleSizes).toLong()
        }
    }

    private fun sampleFor(
        action: String,
        stats: InteractionStats
    ): Sample? {
        val center = stats.centers?.get(action)?.serialize()?.toDouble()
        val sampleSize = stats.sampleSizes?.get(action)

        return if (center != null && sampleSize != null) {
            Sample(center, sampleSize)
        } else {
            null
        }
    }
}