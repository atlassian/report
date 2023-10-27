package com.atlassian.performance.tools.report.jfr

import com.atlassian.performance.tools.report.api.result.CompressedResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkParserListener
import org.openjdk.jmc.flightrecorder.testutils.parser.MetadataEvent
import org.openjdk.jmc.flightrecorder.testutils.parser.RecordingStream
import org.openjdk.jmc.flightrecorder.testutils.parser.StreamingChunkParser
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path

class JfrExecutionEventFilterTest {
    private val zippedInput = File(javaClass.getResource("/profiler-result.zip")!!.toURI())

    data class Chunk(
        val eventsCount: Map<Long, Long>,
        val header: ChunkHeader,
        val metadataEvent: List<MetadataEvent>
    )

    private fun Path.summary(): List<Chunk> {
        val eventsCount = mutableMapOf<Long, Long>()
        var chunkHeader: ChunkHeader? = null
        val metadataEvents = mutableListOf<MetadataEvent>()
        val result = mutableListOf<Chunk>()
        FileInputStream(this.toFile()).use {
            StreamingChunkParser().parse(it, object : ChunkParserListener {

                override fun onChunkStart(chunkIndex: Int, header: ChunkHeader): Boolean {
                    println("Chunk $chunkIndex: $header")
                    chunkHeader = header
                    return true
                }

                override fun onChunkEnd(chunkIndex: Int, skipped: Boolean): Boolean {
                    result.add(Chunk(eventsCount.toMap(), chunkHeader!!, ArrayList(metadataEvents)))
                    eventsCount.clear()
                    metadataEvents.clear()
                    println("Chunk $chunkIndex end")
                    return true
                }

                override fun onMetadata(metadata: MetadataEvent): Boolean {
                    println("metadata: $metadata")
                    metadataEvents.add(metadata)
                    return true
                }

                override fun onEvent(
                    typeId: Long,
                    stream: RecordingStream,
                    payloadSize: Long
                ): Boolean {
                    eventsCount.computeIfAbsent(typeId) { 0 }
                    eventsCount[typeId] = eventsCount[typeId]!! + 1
                    return true
                }

            })
        }
        return result
    }

    @Test
    fun shouldRewriteJfr() {
        // given
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        val expectedSummary = input.toAbsolutePath().summary()
        // when
        val output = JfrExecutionEventFilter().go(input)
        // then
        val firstChunk = expectedSummary.first()
        assertThat(firstChunk.eventsCount[101]).isEqualTo(7731677)
        assertThat(firstChunk.eventsCount[106]).isEqualTo(1023)
        val actualSummary = output.toPath().summary()
        assertThat(actualSummary).isEqualTo(expectedSummary)
    }

}
