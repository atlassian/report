package com.atlassian.performance.tools.report.jstat

import org.assertj.core.api.Assertions
import org.junit.Test

class JstatGcutilParserTest {
    private val system = "test"

    @Test
    fun shouldParseJstat() {
        val jstatCsv = this::class.java.getResourceAsStream("./jstat.csv")

        val metrics = JstatGcutilParser().parse(jstatCsv, system)

        Assertions.assertThat(metrics.map { it.toString() })
            .contains(
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_SURVI_0, value=0.0, system='test')",
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_SURVI_1, value=0.0, system='test')",
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_EDEN, value=42.01, system='test')",
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_OLD, value=0.0, system='test')",
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_META, value=17.29, system='test')",
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_COMPRESSED_CLASS, value=19.76, system='test')",
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_YOUNG_GEN_GC, value=0.0, system='test')",
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_YOUNG_GEN_GC_TIME, value=0.0, system='test')",
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_FULL_GC, value=0.0, system='test')",
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_FULL_GC_TIME, value=0.0, system='test')",
                "SystemMetric(start=2018-01-18T13:58:41Z, dimension=JSTAT_TOTAL_GC_TIME, value=0.0, system='test')",
                "SystemMetric(start=2018-01-18T13:58:43Z, dimension=JSTAT_SURVI_0, value=0.0, system='test')"
            )
    }

    @Test
    fun shouldNotRelyOnColumnsOrder() {
        val jstatCsv = this::class.java.getResourceAsStream("./jstat.csv")
        val jstatShuffledCsv = this::class.java.getResourceAsStream("./jstat_shuffled.csv")

        val metrics = JstatGcutilParser().parse(jstatCsv, system)
        val shuffledMetrics = JstatGcutilParser().parse(jstatShuffledCsv, system)

        Assertions.assertThat(metrics.map { it.toString() })
            .containsAll(shuffledMetrics.map { it.toString() })
    }
}
