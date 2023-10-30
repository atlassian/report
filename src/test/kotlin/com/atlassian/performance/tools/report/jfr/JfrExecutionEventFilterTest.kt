package com.atlassian.performance.tools.report.jfr

import com.atlassian.performance.tools.report.api.result.CompressedResult
import org.apache.logging.log4j.LogManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openjdk.jmc.flightrecorder.testutils.parser.*
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.time.Instant
import java.util.function.Predicate

class JfrExecutionEventFilterTest {
    private val logger = LogManager.getLogger(this::class.java)
    private val zippedInput = File(javaClass.getResource("/profiler-result.zip")!!.toURI())

    data class Chunk(
        val eventsCount: Map<Long, Long>,
        val header: ChunkHeader,
        val metadataEvents: List<MetadataEvent>,
        val eventTypes: List<Long>,
        val eventSizes: List<Long>
    ) {
        override fun toString(): String {
            return "Chunk(eventsCount=$eventsCount, header=$header, metadataEvent=$metadataEvents)"
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
                            metadataEvents = ArrayList(metadataEvents),
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

                override fun onMetadata(
                    eventHeader: EventHeader,
                    metadataPayload: ByteArray,
                    metadata: MetadataEvent
                ): Boolean {
                    logger.debug("$metadata")
                    metadataEvents.add(metadata)
                    return true
                }

                override fun onEvent(eventHeader: EventHeader, eventPayload: ByteArray): Boolean {
                    eventsCount.computeIfAbsent(eventHeader.eventTypeId) { 0 }
                    eventsCount[eventHeader.eventTypeId] = eventsCount[eventHeader.eventTypeId]!! + 1
                    eventTypes.add(eventHeader.eventTypeId)
                    eventSizes.add(eventPayload.size.toLong())
                    return true
                }

            })
        }
        return result
    }

    private fun expectedSummary(input: Path): List<Chunk> {
        val expectedSummary = input.toAbsolutePath().summary()
        val firstChunk = expectedSummary.first()
        assertThat(firstChunk.eventsCount[101]).isEqualTo(7731677)
        assertThat(firstChunk.eventsCount[106]).isEqualTo(1023)
        return expectedSummary
    }

    @Test
    fun shouldRewriteJfr() {
        // given
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        logger.debug("Reading expected JFR $input ...")
        val expectedSummary = expectedSummary(input)
        // when
        logger.debug("Rewriting JFR without changes ...")
        val output = JfrExecutionEventFilter().filter(input)
        // then
        logger.debug("Reading actual JFR $output ...")
        val actualSummary = output.toPath().summary()
        assertThat(actualSummary).isEqualTo(expectedSummary)
    }

    @Test
    fun shouldParseFirstExecutionEventTimestamp() {
        // given
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        var firstEvent: Event? = null
        var lastEvent: Event? = null
        val predicate = Predicate<Event> {
            if (it.header.eventTypeId == 101L) {
                if (firstEvent == null) {
                    firstEvent = it
                }
                lastEvent = it
            }
            true
        }
        // when
        logger.debug("Filtering JFR ...")
        val output = JfrExecutionEventFilter(predicate).filter(input)
        // then
        logger.debug("Reading actual JFR $output ...")
        val actualSummary = output.toPath().summary()
        val gmtOffset = actualSummary.first().metadataEvents.first().gmtOffset
        assertThat(gmtOffset).isEqualTo(0)

        assertThat(firstEvent!!.start).isEqualTo(Instant.parse("2023-10-25T07:14:03.518352946Z"))
//        assertThat(lastEvent!!.start).isEqualTo(Instant.parse("2023-10-25T07:14:03.518352946Z"))
    }

    @Test
    fun shouldFilterEventsBetweenTimestamps() {
        // given
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        val earliestEvent = Instant.parse("2023-10-25T07:14:03.518352946Z")
        val latestEvent = earliestEvent.plusSeconds(60)
        var firstEvent: Event? = null
        var lastEvent: Event? = null
        val predicate = Predicate<Event> {
            val isEventAccepted = it.start.isAfter(earliestEvent) && it.start.isBefore(latestEvent)
            if (isEventAccepted) {
                if (firstEvent == null) {
                    firstEvent = it
                }
                lastEvent = it
            }
            isEventAccepted
        }
        // when
        logger.debug("Filtering JFR ...")
        val jrfFilter = JfrExecutionEventFilter(predicate)
        val output = jrfFilter.filter(input)
        // then
        logger.debug("Reading actual JFR $output ...")
        val actualSummary = output.toPath().summary()

        assertThat(firstEvent!!.start).isEqualTo(Instant.parse("2023-10-25T07:14:03.518352946Z"))
        assertThat(lastEvent!!.start).isEqualTo(Instant.parse("2023-10-25T07:14:03.518352946Z"))
    }


}
