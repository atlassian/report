package com.atlassian.performance.tools.report.jfr

import jdk.jfr.consumer.RecordedEvent
import org.apache.logging.log4j.LogManager
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkParserListener
import org.openjdk.jmc.flightrecorder.testutils.parser.EventHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.MetadataEvent
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.function.Predicate

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

        override fun onChunkStart(chunkIndex: Int, header: ChunkHeader) {
            lastHeader = header
            absoluteChunkStartPos = countingOutput.count
            header.write(output)
        }

        override fun onMetadata(
            eventHeader: EventHeader,
            metadataPayload: ByteArray,
            metadata: MetadataEvent
        ) {
            lastMetadataEventOffset = countingOutput.countSinceLastReset

            output.write(eventHeader.bytes)
            output.write(metadataPayload)
        }

        override fun onEvent(event: RecordedEvent, eventHeader: EventHeader, eventPayload: ByteArray) {
            if (eventFilter.test(event)) {
                output.write(eventHeader.bytes)
                output.write(eventPayload)
            }
        }

        override fun onCheckpoint(eventHeader: EventHeader, eventPayload: ByteArray) {
            lastCheckpointEventOffset = countingOutput.countSinceLastReset
            output.write(eventHeader.bytes)
            output.write(eventPayload)
        }

        override fun onChunkEnd(chunkIndex: Int, skipped: Boolean) {
            updateChunk()
            countingOutput.resetCount()
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
