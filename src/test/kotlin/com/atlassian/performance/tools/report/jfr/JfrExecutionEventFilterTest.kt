package com.atlassian.performance.tools.report.jfr

import com.atlassian.performance.tools.report.api.result.CompressedResult
import org.apache.logging.log4j.LogManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkParserListener
import org.openjdk.jmc.flightrecorder.testutils.parser.MetadataEvent
import org.openjdk.jmc.flightrecorder.testutils.parser.StreamingChunkParser
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path

class JfrExecutionEventFilterTest {
    private val logger = LogManager.getLogger(this::class.java)
    private val zippedInput = File(javaClass.getResource("/profiler-result.zip")!!.toURI())

    data class Chunk(
        val eventsCount: Map<Long, Long>,
        val header: ChunkHeader,
        val metadataEvent: List<MetadataEvent>,
        val eventTypes: List<Long>,
        val eventSizes: List<Long>
    ) {
        override fun toString(): String {
            return "Chunk(eventsCount=$eventsCount, header=$header, metadataEvent=$metadataEvent)"
        }
    }



    private fun Path.summary(): List<Chunk> {
        val eventsCount = mutableMapOf<Long, Long>()
        var chunkHeader: ChunkHeader? = null
        val metadataEvents = mutableListOf<MetadataEvent>()
        val eventTypes = mutableListOf<Long>()
        val eventSizes = mutableListOf<Long>()
        val result = mutableListOf<Chunk>()
        FileInputStream(this.toFile()).use {
            StreamingChunkParser().parse(it, object : ChunkParserListener {

                override fun onChunkStart(chunkIndex: Int, header: ChunkHeader): Boolean {
                    logger.debug("Chunk $chunkIndex>>")
                    logger.debug("$header")
                    chunkHeader = header
                    return true
                }

                override fun onChunkEnd(chunkIndex: Int, skipped: Boolean): Boolean {
                    logger.debug("eventsCount: $eventsCount")
                    logger.debug("<<Chunk $chunkIndex")
                    result.add(
                        Chunk(
                            eventsCount = eventsCount.toMap(),
                            header = chunkHeader!!,
                            metadataEvent = ArrayList(metadataEvents),
                            eventTypes = ArrayList(eventTypes),
                            eventSizes = ArrayList(eventSizes)
                        )
                    )
                    eventsCount.clear()
                    metadataEvents.clear()
                    eventTypes.clear()
                    eventSizes.clear()
                    return true
                }

                override fun onMetadata(eventSize: Long, eventTypeId: Long, metadata: MetadataEvent): Boolean {
                    logger.debug("$metadata")
                    metadataEvents.add(metadata)
                    return true
                }

                override fun onEvent(
                    eventSize: Long,
                    eventTypeId: Long,
                    eventPayload: ByteArray
                ): Boolean {
                    eventsCount.computeIfAbsent(eventTypeId) { 0 }
                    eventsCount[eventTypeId] = eventsCount[eventTypeId]!! + 1
                    eventTypes.add(eventTypeId)
                    eventSizes.add(eventPayload.size.toLong())
                    return true
                }

            })
        }
        return result
    }

    private fun expectedSummary(input: Path) :List<Chunk> {
        val expectedSummary = input.toAbsolutePath().summary()
        val firstChunk = expectedSummary.first()
        assertThat(firstChunk.eventsCount[101]).isEqualTo(7731677)
        assertThat(firstChunk.eventsCount[106]).isEqualTo(1023)
        return expectedSummary
    }

    @Test
    fun shouldPrintDetails() {
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        val expectedSummary = input.toAbsolutePath().summary()
        logger.debug(expectedSummary)
    }

    @Test
    fun shouldRewriteJfr() {
        // given
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        logger.debug("Reading expected JRF $input ...")
        val expectedSummary = expectedSummary(input)
        // when
        logger.debug("Filtering JRF...")
        val output = JfrExecutionEventFilter().filter(input)
        // then
        logger.debug("Reading actual JRF $output ...")
        val actualSummary = output.toPath().summary()
        assertThat(actualSummary).isEqualTo(expectedSummary)
    }

}
