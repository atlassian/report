package com.atlassian.performance.tools.report.api.jfr

import com.atlassian.performance.tools.report.jfr.FilteringJfrWriter
import jdk.jfr.consumer.RecordedEvent
import org.openjdk.jmc.flightrecorder.testutils.parser.*
import tools.profiler.jfr.converter.CheckpointEvent
import java.io.Closeable
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Creates multiple JFR files from a single JFR file.
 * MultiJfrFilter.filter() with n outputs is faster than n x JfrFilter.filter()
 */
class MultiJfrFilter private constructor(
    private val input: Path,
    private val outputs: List<FilteredOutput>,
    private val symbolModifier: Consumer<MutableJvmSymbol>
) {

    private class CompositeJfrEventListener(
        private val listeners: List<ChunkParserListener>
    ) : ChunkParserListener {

        override fun onChunkStart(chunkIndex: Int, header: ChunkHeader) {
            listeners.forEach { it.onChunkStart(chunkIndex, header) }
        }

        override fun onChunkEnd(chunkIndex: Int, skipped: Boolean) {
            listeners.forEach { it.onChunkEnd(chunkIndex, skipped) }
        }

        override fun onMetadata(
            eventHeader: EventHeader,
            metadataPayload: ByteArray,
            metadata: MetadataEvent
        ) {
            listeners.forEach { it.onMetadata(eventHeader, metadataPayload, metadata) }
        }

        override fun onEvent(event: RecordedEvent, eventHeader: EventHeader, eventPayload: ByteArray) {
            listeners.forEach { it.onEvent(event, eventHeader, eventPayload) }
        }

        override fun onCheckpoint(eventHeader: EventHeader, eventPayload: ByteArray, checkpoint: CheckpointEvent) {
            listeners.forEach { it.onCheckpoint(eventHeader, eventPayload, checkpoint) }
        }

        override fun onRecordingStart() {
            listeners.forEach { it.onRecordingStart() }
        }

        override fun onRecordingEnd() {
            listeners.forEach { it.onRecordingEnd() }
        }

    }

    fun filter() {
        val streams = mutableListOf<Closeable>()
        try {
            val writers = outputs.map {
                val stream = it.path.toFile().outputStream().buffered()
                streams.add(stream)
                FilteringJfrWriter(it.path.toFile(), stream, it.filter)
            }
            val compositeListener = CompositeJfrEventListener(writers)
            val parser = StreamingChunkParser(compositeListener, symbolModifier::accept)
            parser.parse(input)
        } finally {
            streams.forEach { it.close() }
        }
    }

    private class FilteredOutput(
        val path: Path,
        val filter: Predicate<RecordedEvent>
    )

    class Builder(
        private val input: Path
    ) {
        private val outputs = mutableListOf<FilteredOutput>()
        private var symbolModifier: Consumer<MutableJvmSymbol> = Consumer { }

        fun output(output: Path, filter: Predicate<RecordedEvent>): Builder {
            outputs.add(FilteredOutput(output, filter))
            return this
        }

        fun symbolModifier(symbolModifier: Consumer<MutableJvmSymbol>) = apply { this.symbolModifier = symbolModifier }

        fun build(): MultiJfrFilter {
            check(outputs.isNotEmpty()) { "At least one output must be specified" }
            return MultiJfrFilter(input, outputs, symbolModifier)
        }
    }

}
