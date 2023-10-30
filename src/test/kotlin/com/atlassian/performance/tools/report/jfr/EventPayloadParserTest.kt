package com.atlassian.performance.tools.report.jfr

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.EventHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.RecordingStream
import java.io.ByteArrayInputStream
import java.time.Instant

class EventPayloadParserTest {
    // @formatter:off
    /**
     * Taken from real JFR file
     */
    private val chunkHeaderBytes = byteArrayOf(70, 76, 82, 0, 0, 2, 0, 0, 0, 0, 0, 0, 7, -59, 23, -103, 0, 0, 0, 0, 6, 71, -32, -46, 0, 0, 0, 0, 0, 0, 0, 68, 23, -111, 72, -46, -28, -90, -49, 104, 0, 0, 0, -18, 53, -95, -124, 16, 0, 0, 0, -126, -63, -108, -45, 106, 0, 0, 0, 0, 59, -102, -54, 0, -108, -80, -128, -128)

    /**
     * Taken from real JFR file
     */
    private val eventPayload = byteArrayOf(-101, -99, -84, -40, -82, 16, -61, 67, -69, -2, 3, 2)
    // @formatter:on

    @Test
    fun shouldParseEventPayload() {
        // given
        val eventPayloadParser = EventPayloadParser()
        val chunkHeader = ChunkHeader.read(RecordingStream(ByteArrayInputStream(chunkHeaderBytes)))
        // when
        val event = eventPayloadParser.parse(
            chunkHeader, EventHeader(0L, 0, byteArrayOf()), eventPayload
        )
        // then
        assertThat(event.start).isEqualTo(Instant.parse("2023-10-25T07:14:03.518356121Z"))
    }

}
