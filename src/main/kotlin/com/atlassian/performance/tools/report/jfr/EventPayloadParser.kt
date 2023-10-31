package com.atlassian.performance.tools.report.jfr

import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.EventHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.RecordingStream
import java.io.ByteArrayInputStream
import java.time.Instant

class EventPayloadParser {
    private val timeConverters = mutableMapOf<ChunkHeader, TimeConverter>()

    fun parse(chunkHeader: ChunkHeader, header: EventHeader, payload: ByteArray): Event {
        val timeConverter = timeConverters.computeIfAbsent(chunkHeader) { TimeConverter.of(it) }
        val startTick = RecordingStream(ByteArrayInputStream(payload)).readVarint()
        val eventStart = Instant.ofEpochSecond(0, timeConverter.convertTimestamp(startTick))
        return Event(eventStart, header, null)
    }
}
