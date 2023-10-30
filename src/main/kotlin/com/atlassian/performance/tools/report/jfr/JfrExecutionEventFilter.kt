package com.atlassian.performance.tools.report.jfr

import org.apache.logging.log4j.LogManager
import org.openjdk.jmc.flightrecorder.testutils.parser.*
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.file.Path
import java.util.function.Predicate


class JfrExecutionEventFilter(
    private val eventFilter: Predicate<EventHeader> = Predicate { true }
) {
    private val logger = LogManager.getLogger(this::class.java)

    fun filter(recording: Path): File {
        recording.toFile().inputStream().buffered().use { inputStream ->
            val filteredRecording = recording.resolveSibling("filtered-" + recording.fileName.toString()).toFile()
            logger.debug("Writing filtered recording to $filteredRecording ...")
            filteredRecording.outputStream().buffered().use { outputStream ->
                val writer = FilteringJfrWriter(filteredRecording, outputStream, eventFilter)
                val parser = StreamingChunkParser()
                parser.parse(inputStream, writer)
            }
            return filteredRecording
        }
    }

    class FilteringJfrWriter(
        private val outputFile: File,
        output: OutputStream,
        private val eventFilter: Predicate<EventHeader>
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

        override fun onEvent(eventHeader: EventHeader, eventPayload: ByteArray): Boolean {
            if (eventHeader.eventTypeId == checkpointEventType) {
                lastCheckpointEventOffset = countingOutput.countSinceLastReset
            }

            if (eventFilter.test(eventHeader)) {
                output.write(eventHeader.bytes)
                output.write(eventPayload)
            }
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
