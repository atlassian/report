package com.atlassian.performance.tools.report.jfr

import com.atlassian.performance.tools.report.api.jfr.JfrFilter
import com.atlassian.performance.tools.report.api.jfr.MultiJfrFilter
import com.atlassian.performance.tools.report.api.result.CompressedResult
import jdk.jfr.consumer.RecordedEvent
import org.apache.logging.log4j.LogManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.Path
import java.util.function.Function
import java.util.function.Predicate
import kotlin.system.measureTimeMillis

class MultiJfrFilterTest {
    private val logger = LogManager.getLogger(this::class.java)

    @Test
    fun shouldBeFasterThanSingle() {
        // given
        val zippedInput = File(javaClass.getResource("/profiler-result.zip")!!.toURI())
        val sampleInput = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")

        val multiFilterBuilder = MultiJfrFilter.Builder(sampleInput)
        val singleJfrFilters = mutableListOf<JfrFilter>()
        repeat(10) { index ->
            val output =
                sampleInput.resolveSibling("shouldBeFasterThanSingle-$index-" + sampleInput.fileName.toString())
            val filter = Predicate<RecordedEvent> {
                it.javaThreadId() == 670L
            }
            multiFilterBuilder.output(output, filter)
            singleJfrFilters.add(
                JfrFilter.Builder()
                    .eventFilter(filter)
                    .filteredRecording(Function { _: Path -> output })
                    .build()
            )
        }
        val multiFilter = multiFilterBuilder.build()
        // when
        logger.debug("Filtering JFR using MultiJfrFilter implementation ...")
        val multiDuration = measureTimeMillis {
            multiFilter.filter()
        }
        logger.debug("MultiJfrFilter duration: $multiDuration ms")

        logger.debug("Filtering JFR using JfrFilter implementation ...")
        val singleDuration = measureTimeMillis {
            singleJfrFilters.forEach { it.filter(sampleInput) }
        }
        logger.debug("SingleJfrFilters duration: $singleDuration ms")
        // then
        assertThat(multiDuration)
            .describedAs("Multi duration should be at least 60% faster than single - given enough outputs to produce")
            .isLessThan((singleDuration * 0.4).toLong())
    }
}
