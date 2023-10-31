package com.atlassian.performance.tools.report.jfr

import com.atlassian.performance.tools.report.api.result.CompressedResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkParserListener
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
        var firstEvent: Event? = null
        var lastEvent: Event? = null
        parser.parse(input, object : ChunkParserListener {
            override fun onEvent(event: Event, eventPayload: ByteArray): Boolean {
                if (firstEvent == null) {
                    firstEvent = event
                }
                lastEvent = event
                return true
            }
        });
        // then
        assertThat(firstEvent!!.start).isEqualTo(Instant.parse("2023-10-25T07:30:48.370264532Z"))
        assertThat(lastEvent!!.start).isEqualTo(Instant.parse("2023-10-25T07:23:25.111857Z"))
    }


}
