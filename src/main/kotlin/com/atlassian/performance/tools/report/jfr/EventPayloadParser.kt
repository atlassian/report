package com.atlassian.performance.tools.report.jfr

import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.EventHeader
import java.io.ByteArrayInputStream
import java.time.Instant

class EventPayloadParser {
    private val timeConverters = mutableMapOf<ChunkHeader, TimeConverter>()

    fun byteArrayToLong(bytes: ByteArray): Long {
        var result: Long = 0
        for (byte in bytes) {
            result = (result shl 8) or (byte.toInt() and 0xFF).toLong()
        }
        return result
    }
    fun parse(chunkHeader: ChunkHeader, header: EventHeader, payload: ByteArray): Event {
        val timeConverter = timeConverters.computeIfAbsent(chunkHeader) { TimeConverter.of(it) }
        val startTick = VarIntParser.parse(ByteArrayInputStream(payload))
        val eventStart = Instant.ofEpochSecond(0, timeConverter.convertTimestamp(startTick))
        return Event(eventStart, header)
    }
}
