package com.atlassian.performance.tools.report.jfr

import com.atlassian.performance.tools.report.api.result.CompressedResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkParserListener
import org.openjdk.jmc.flightrecorder.testutils.parser.RecordingStream
import org.openjdk.jmc.flightrecorder.testutils.parser.StreamingChunkParser
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path

class JfrExecutionEventFilterTest {
    private val zippedInput = File(javaClass.getResource("/profiler-result.zip")!!.toURI())

    private fun Path.countEvents(): Int {
        var eventCount = 0
        FileInputStream(this.toFile()).use {
            StreamingChunkParser().parse(it, object : ChunkParserListener {
                override fun onEvent(typeId: Long, stream: RecordingStream, payloadSizeInBytes: Long): Boolean {
                    eventCount++
                    return true
                }
            })
        }
        return eventCount
    }

    @Test
    @Ignore("WIP")
    fun shouldRewriteJfr() {
        // given
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        val expectedEventCount = input.toAbsolutePath().countEvents()
        // when
        val output = JfrExecutionEventFilter().go(input)
        // then
        val actualEventCount = output.toPath().countEvents()
        assertThat(actualEventCount).isEqualTo(expectedEventCount)
    }

}
