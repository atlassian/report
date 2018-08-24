package com.atlassian.performance.tools.report.vmstat

import com.atlassian.performance.tools.infrastructure.api.metric.Dimension
import com.atlassian.performance.tools.infrastructure.api.metric.SystemMetric
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.InputStream
import java.time.*

internal class VmstatParser {

    internal fun parse(
        inputStream: InputStream,
        system: String
    ): List<SystemMetric> {
        return CSVParser(
            inputStream.bufferedReader(),
            CSVFormat.DEFAULT.withHeader(VmstatHeader::class.java)
        )
            .asSequence()
            .map { record -> parseRecord(record, system) }
            .toList()
    }

    private fun parseRecord(
        record: CSVRecord,
        system: String
    ): SystemMetric {
        return SystemMetric(
            start = parseStart(record),
            dimension = Dimension.CPU_LOAD,
            value = parseValue(record),
            system = system
        )
    }

    private fun parseStart(record: CSVRecord): Instant {
        return ZonedDateTime.of(
            LocalDate.parse(record.get(VmstatHeader.DATE)),
            LocalTime.parse(record.get(VmstatHeader.TIME)),
            ZoneId.of("UTC")
        ).toInstant()
    }

    private fun parseValue(record: CSVRecord) =
        record.get(VmstatHeader.US).toDouble() + record.get(VmstatHeader.SY).toDouble()
}