package com.atlassian.performance.tools.report.jfr

import com.atlassian.performance.tools.report.api.jfr.JfrFilter
import com.atlassian.performance.tools.report.api.result.CompressedResult
import jdk.jfr.consumer.RecordedEvent
import org.apache.logging.log4j.LogManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openjdk.jmc.flightrecorder.testutils.parser.*
import tools.profiler.jfr.converter.CheckpointEvent
import java.io.File
import java.io.StringWriter
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Predicate

class JfrFilterTest {
    private val logger = LogManager.getLogger(this::class.java)
    private val zippedInput = File(javaClass.getResource("/profiler-result.zip")!!.toURI())

    data class Chunk(
        val eventsCount: Map<Long, Long>,
        val header: ChunkHeader,
        val metadataEvents: List<MetadataEvent>,
        val eventTypes: List<Long>,
        val eventSizes: List<Long>,
        val symbolCount: Int
    ) {
        override fun toString(): String {
            return "Chunk(eventsCount=$eventsCount, header=$header, metadataEvent=$metadataEvents, symbolCount=$symbolCount)"
        }
    }


    private fun Path.summary(): List<Chunk> {
        val eventsCount = mutableMapOf<Long, Long>()
        var chunkHeader: ChunkHeader? = null
        val metadataEvents = mutableListOf<MetadataEvent>()
        val eventTypes = mutableListOf<Long>()
        val eventSizes = mutableListOf<Long>()
        val result = mutableListOf<Chunk>()
        val symbolCount = AtomicInteger(0)
        val chunkListener = object : ChunkParserListener {

            override fun onChunkStart(chunkIndex: Int, header: ChunkHeader) {
                logger.debug("Chunk $chunkIndex>>")
                logger.debug("$header")
                chunkHeader = header
            }

            override fun onChunkEnd(chunkIndex: Int, skipped: Boolean) {
                logger.debug("eventsCount: $eventsCount")
                logger.debug("<<Chunk $chunkIndex")
                result.add(
                    Chunk(
                        eventsCount = eventsCount.toMap(),
                        header = chunkHeader!!,
                        metadataEvents = ArrayList(metadataEvents),
                        eventTypes = ArrayList(eventTypes),
                        eventSizes = ArrayList(eventSizes),
                        symbolCount = symbolCount.get()
                    )
                )
                eventsCount.clear()
                metadataEvents.clear()
                eventTypes.clear()
                eventSizes.clear()
                symbolCount.set(0)
            }

            override fun onMetadata(
                eventHeader: EventHeader,
                metadataPayload: ByteArray,
                metadata: MetadataEvent
            ) {
                logger.debug("$metadata")
                metadataEvents.add(metadata)
            }

            override fun onEvent(event: RecordedEvent, header: EventHeader, eventPayload: ByteArray) {
                eventsCount.computeIfAbsent(header.eventTypeId) { 0 }
                eventsCount[header.eventTypeId] = eventsCount[header.eventTypeId]!! + 1
                eventTypes.add(header.eventTypeId)
                eventSizes.add(eventPayload.size.toLong())
            }

        }
        val checkpointListener = CheckpointEvent.Listener { symbolCount.incrementAndGet() }
        StreamingChunkParser(chunkListener, checkpointListener).parse(this)
        return result
    }

    private fun expectedSummary(input: Path): List<Chunk> {
        val expectedSummary = input.toAbsolutePath().summary()
        val firstChunk = expectedSummary.first()
        assertThat(firstChunk.eventsCount[101]).isEqualTo(7731677)
        assertThat(firstChunk.eventsCount[106]).isEqualTo(1023)
        assertThat(firstChunk.symbolCount).isEqualTo(31380)
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
        val output = JfrFilter.Builder().build().filter(input)
        // then
        logger.debug("Reading actual JFR $output ...")
        val actualSummary = output.toPath().summary()
        assertThat(actualSummary).isEqualTo(expectedSummary)
    }

    @Test
    fun shouldRewriteJfrSymbols() {
        // given
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        val expectedSummary = expectedSummary(input)

        // when
        val output = JfrFilter.Builder()
            .symbolModifier(Consumer(this::normalizeDynamicProxy))
            .build()
            .filter(input)

        // then
        logger.debug("Reading actual JFR $output ...")
        val actualSummary = output.toPath().summary()
        assertThat(actualSummary).isEqualTo(expectedSummary) // TODO assert on the actual normalization
    }

    /**
     * Normalize symbols like:
     * - `jdk.proxy3.$Proxy587.doInTransaction(HostContextAccessor$HostTransactionCallback)`
     * - `jdk.proxy3.$Proxy478.doesQueryFitFilterForm(ApplicationUser, Query)`
     */
    private fun normalizeDynamicProxy(symbolPayload: ByteArray) {
        if (String(symbolPayload).contains("Proxy")) { //TODO only for $Proxy, watch out for regex in [contains]
            val newSymbol = "PROXY".padEnd(symbolPayload.size, '_') // TODO preserve the method signature (after the last dot)
            ByteBuffer.wrap(symbolPayload).put(newSymbol.toByteArray())
        }
    }

    @Test
    fun shouldFilterByThreadId() {
        // given
        val input = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        val expectedThreadCounter = mutableMapOf<Long?, Long>()
        val predicateBefore = Predicate<RecordedEvent> {
            val javaThreadId = it.javaThreadId()
            expectedThreadCounter.compute(javaThreadId) { _, count -> (count ?: 0) + 1 }
            javaThreadId == 670L
        }
        // when
        logger.debug("Filtering JFR ...")
        val jrfFilter = JfrFilter.Builder().eventFilter(predicateBefore).build()
        val filteredFile = jrfFilter.filter(input)
        // then
        val actualThreadCounter = mutableMapOf<Long?, Long>()
        StreamingChunkParser(object : ChunkParserListener {
            override fun onEvent(event: RecordedEvent, header: EventHeader, eventPayload: ByteArray) {
                val javaThreadId = event.javaThreadId()
                actualThreadCounter.compute(javaThreadId) { _, count -> (count ?: 0) + 1 }
            }
        }).parse(filteredFile.toPath())
        assertThat(actualThreadCounter[670L]).isEqualTo(expectedThreadCounter[670L])
        actualThreadCounter.remove(670L)
        assertThat(actualThreadCounter).isEmpty()
    }

}
