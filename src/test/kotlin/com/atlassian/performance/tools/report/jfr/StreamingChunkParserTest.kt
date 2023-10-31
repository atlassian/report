package com.atlassian.performance.tools.report.jfr

import com.atlassian.performance.tools.report.api.result.CompressedResult
import jdk.jfr.consumer.RecordedEvent
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkParserListener
import org.openjdk.jmc.flightrecorder.testutils.parser.EventHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.StreamingChunkParser
import java.io.File
import java.time.Instant

class StreamingChunkParserTest {
    private val zippedInput = File(javaClass.getResource("/profiler-result.zip")!!.toURI())

    @Test
    fun shouldParseTimestamps() {
        // given
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        val parser = StreamingChunkParser()
        // when
        var firstEvent: RecordedEvent? = null
        var lastEvent: RecordedEvent? = null
        parser.parse(input, object : ChunkParserListener {
            override fun onEvent(event: RecordedEvent, header: EventHeader, eventPayload: ByteArray): Boolean {
                if (firstEvent == null) {
                    firstEvent = event
                }
                lastEvent = event
                return true
            }
        });
        // then
        SoftAssertions.assertSoftly {
            it.assertThat(firstEvent!!.startTime).isEqualTo(Instant.parse("2023-10-25T07:23:25.111857Z"))
            it.assertThat(lastEvent!!.startTime).isEqualTo(Instant.parse("2023-10-25T07:48:28.237893641Z"))
        }
    }

    @Test
    fun shouldParseThreadIds() {
        // given
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        val parser = StreamingChunkParser()
        var foundThreads = 0
        var foundExecutionSamples = 0
        val threadIdCount = mutableMapOf<Long, Int>()
        // when
        parser.parse(input, object : ChunkParserListener {
            override fun onEvent(event: RecordedEvent, header: EventHeader, eventPayload: ByteArray): Boolean {
                if (event.eventType.id == 101L) {
                    foundExecutionSamples++
                    val javaThreadId = event.javaThreadId()
                    if (javaThreadId != null) {
                        threadIdCount.compute(javaThreadId) { _, count -> (count ?: 0) + 1 }
                        foundThreads++
                    }
                }
                return true
            }
        });
        // then
        SoftAssertions.assertSoftly {
            it.assertThat(foundThreads).isEqualTo(foundExecutionSamples)
            it.assertThat(threadIdCount[103]).isEqualTo(1468)
            it.assertThat(threadIdCount[883]).isEqualTo(22506)
            it.assertThat(threadIdCount[954]).isEqualTo(1)
        }
    }


}
