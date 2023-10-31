package com.atlassian.performance.tools.report.jfr

import jdk.jfr.consumer.RecordedEvent
import org.apache.logging.log4j.LogManager
import org.openjdk.jmc.flightrecorder.testutils.parser.*
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.file.Path
import java.util.function.Predicate


class JfrFilter(
    private val eventFilter: Predicate<RecordedEvent> = Predicate { true }
) {
    private val logger = LogManager.getLogger(this::class.java)

    fun filter(recording: Path): File {
        val filteredRecording = recording.resolveSibling("filtered-" + recording.fileName.toString()).toFile()
        logger.debug("Writing filtered recording to $filteredRecording ...")
        filteredRecording.outputStream().buffered().use { outputStream ->
            val writer = FilteringJfrWriter(filteredRecording, outputStream, eventFilter)
            val parser = StreamingChunkParser()
            parser.parse(recording, writer)
        }
        return filteredRecording
    }

    class FilteringJfrWriter(
        private val outputFile: File,
        output: OutputStream,
        private val eventFilter: Predicate<RecordedEvent>
    ) : ChunkParserListener {
        private val logger = LogManager.getLogger(this::class.java)


        private val countingOutput = CountingOutputStream(output)

        private val output = DataOutputStream(countingOutput)

        private var lastHeader: ChunkHeader? = null
        private var lastMetadataEventOffset: Long = 0L
        private var lastCheckpointEventOffset: Long = 0L
        private var absoluteChunkStartPos = 0L

        override fun onChunkStart(chunkIndex: Int, header: ChunkHeader): Boolean {
            lastHeader = header
            absoluteChunkStartPos = countingOutput.count
            header.write(output)
            return true
        }

        private val checkpointEventType = 1L

        override fun onMetadata(
            eventHeader: EventHeader,
            metadataPayload: ByteArray,
            metadata: MetadataEvent
        ): Boolean {
            lastMetadataEventOffset = countingOutput.countSinceLastReset

            output.write(eventHeader.bytes)
            output.write(metadataPayload)
            return true
        }

        override fun onEvent(event: RecordedEvent, eventHeader: EventHeader, eventPayload: ByteArray): Boolean {
            if (eventFilter.test(event)) {
                output.write(eventHeader.bytes)
                output.write(eventPayload)
            }
            return true
        }

        override fun onCheckpoint(eventHeader: EventHeader, eventPayload: ByteArray): Boolean {
            lastCheckpointEventOffset = countingOutput.countSinceLastReset
            output.write(eventHeader.bytes)
            output.write(eventPayload)
            return true
        }

        override fun onChunkEnd(chunkIndex: Int, skipped: Boolean): Boolean {
            updateChunk()
            countingOutput.resetCount()
            return true
        }

        private fun updateChunk() {
            output.flush() // save the output, otherwise below update will be lost
            val currentHeader = lastHeader!!
            val updatedHeader = currentHeader
                .toBuilder()
                .size(countingOutput.count - absoluteChunkStartPos)
                .cpOffset(lastCheckpointEventOffset)
                .metaOffset(lastMetadataEventOffset)
                .build()
            RandomAccessFile(outputFile, "rw").use {
                it.seek(absoluteChunkStartPos)
                updatedHeader.apply {
                    logger.debug("Updating chunk to $this")
                    write(it)
                }
            }
        }

    }
}
