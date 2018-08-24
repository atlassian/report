package com.atlassian.performance.tools.report.jstat

import com.atlassian.performance.tools.infrastructure.api.metric.Dimension
import com.atlassian.performance.tools.infrastructure.api.metric.SystemMetric
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.InputStream
import java.time.OffsetDateTime

internal class JstatGcutilParser {
    internal fun parse(
        inputStream: InputStream,
        system: String
    ): List<SystemMetric> {
        val parser = CSVParser(
            inputStream.bufferedReader(),
            CSVFormat.DEFAULT.withHeader(JstatGcutilHeader::class.java)
        )
        val sortedJstatMetrics = parser
            .toList()
            .map { record -> JstatMetric(record, system) }
            .sortedBy { it.start }

        var last: CSVRecord? = null
        return sortedJstatMetrics.flatMap {
            val systemMetrics = it.toSystemMetrics(last)
            last = it.record
            systemMetrics
        }
    }

    private class JstatMetric(
        val record: CSVRecord,
        private val system: String
    ) {
        val start = OffsetDateTime.parse(record.get(JstatGcutilHeader.DATE)).toInstant()!!

        fun toSystemMetrics(
            previous: CSVRecord?
        ): List<SystemMetric> {
            return listOf(
                metric(
                    dimension = Dimension.JSTAT_SURVI_0,
                    value = record.get(JstatGcutilHeader.S0).toDouble()
                ),
                metric(
                    dimension = Dimension.JSTAT_SURVI_1,
                    value = record.get(JstatGcutilHeader.S1).toDouble()
                ),
                metric(
                    dimension = Dimension.JSTAT_EDEN,
                    value = record.get(JstatGcutilHeader.E).toDouble()
                ),
                metric(
                    dimension = Dimension.JSTAT_OLD,
                    value = record.get(JstatGcutilHeader.O).toDouble()
                ),
                metric(
                    dimension = Dimension.JSTAT_META,
                    value = record.get(JstatGcutilHeader.M).toDouble()
                ),
                metric(
                    dimension = Dimension.JSTAT_COMPRESSED_CLASS,
                    value = record.get(JstatGcutilHeader.CCS).toDouble()
                ),
                metric(
                    dimension = Dimension.JSTAT_YOUNG_GEN_GC,
                    value = record.get(JstatGcutilHeader.YGC).toDouble() -
                        (previous?.get(JstatGcutilHeader.YGC)?.toDouble() ?: 0.0)
                ),
                metric(
                    dimension = Dimension.JSTAT_YOUNG_GEN_GC_TIME,
                    value = record.get(JstatGcutilHeader.YGCT).toDouble() -
                        (previous?.get(JstatGcutilHeader.YGCT)?.toDouble() ?: 0.0)
                ),
                metric(
                    dimension = Dimension.JSTAT_FULL_GC,
                    value = record.get(JstatGcutilHeader.FGC).toDouble() -
                        (previous?.get(JstatGcutilHeader.FGC)?.toDouble() ?: 0.0)
                ),
                metric(
                    dimension = Dimension.JSTAT_FULL_GC_TIME,
                    value = record.get(JstatGcutilHeader.FGCT).toDouble() -
                        (previous?.get(JstatGcutilHeader.FGCT)?.toDouble() ?: 0.0)
                ),
                metric(
                    dimension = Dimension.JSTAT_TOTAL_GC_TIME,
                    value = record.get(JstatGcutilHeader.GCT).toDouble()
                )
            )
        }

        private fun metric(
            dimension: Dimension,
            value: Double
        ): SystemMetric {
            return SystemMetric(
                start = start,
                dimension = dimension,
                value = value,
                system = system
            )
        }
    }
}