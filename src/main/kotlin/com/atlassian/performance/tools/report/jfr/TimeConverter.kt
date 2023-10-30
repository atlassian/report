package com.atlassian.performance.tools.report.jfr

import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader
import java.time.ZoneOffset

/**
 * Based on [jdk.jfr.consumer.TimeConverter]
 */
class TimeConverter(
    private val startTicks: Long,
    private val startNanos: Long,
    private val divisor: Double
) {
    /**
     * @return nanos
     */
    fun convertTimestamp(ticks: Long): Long {
        // return startNanos + (long) ((ticks - startTicks) / divisor); //1698218605111857000
        return startNanos + ((ticks - startTicks) / divisor).toLong()
    }

    /**
     * @return nanos
     */
    fun convertTimespan(ticks: Long): Long {
        return (ticks / divisor).toLong()
    }

    companion object {
        fun of(chunkHeader: ChunkHeader): TimeConverter {
            return TimeConverter(
                startTicks = chunkHeader.startTicks,
                startNanos = chunkHeader.startNanos,
                divisor = chunkHeader.frequency.toDouble() / 1000_000_000L
            )
        }
    }
}
