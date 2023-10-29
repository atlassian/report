package com.atlassian.performance.tools.report.jfr

import org.apache.logging.log4j.LogManager
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkParserListener
import org.openjdk.jmc.flightrecorder.testutils.parser.MetadataEvent
import org.openjdk.jmc.flightrecorder.testutils.parser.StreamingChunkParser
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.file.Path


class JfrExecutionEventFilter {
    private val logger = LogManager.getLogger(this::class.java)

    fun filter(recording: Path): File {
        recording.toFile().inputStream().buffered().use { inputStream ->
            val filteredRecording = recording.resolveSibling("filtered-" + recording.fileName.toString()).toFile()
            logger.debug("Writing filtered recording to $filteredRecording ...")
            filteredRecording.outputStream().buffered().use { outputStream ->
                val writer = FilteringJfrWriter(filteredRecording, outputStream, recording)
                val parser = StreamingChunkParser()
                parser.parse(inputStream, writer)
            }
            return filteredRecording
        }
    }

    class FilteringJfrWriter(
        val outputFile: File,
        output: OutputStream,
        val input: Path
    ) : ChunkParserListener {
        private val logger = LogManager.getLogger(this::class.java)

        private val countingOutput = CountingOutputStream(output)
        private val output = DataOutputStream(countingOutput)

        private var lastHeader: ChunkHeader? = null
        private var absoluteChunkStartPos = 0L

        override fun onChunkStart(chunkIndex: Int, header: ChunkHeader): Boolean {
            lastHeader = header
            absoluteChunkStartPos = countingOutput.count
            header.write(output)
            return true
        }

        private val checkpointEventType = 1L

        override fun onEventSize(eventSize: Long) {
            VarInt.write(eventSize, output)
        }

        override fun onEventType(eventType: Long) {
            VarInt.write(eventType, output)
        }

        override fun onMetadata(metadata: MetadataEvent): Boolean {
            val positionBeforeWrite = countingOutput.count
            input.toFile().inputStream().use { inputStream ->
                inputStream.skip(metadata.positionBeforeRead)
                val eventPayload = ByteArray(metadata.payloadSize.toInt())
                inputStream.read(eventPayload)
                output.write(eventPayload)
            }
            val metadataBytesCount = countingOutput.count - positionBeforeWrite

            return true
        }

        override fun onEvent(typeId: Long, eventPayload: ByteArray): Boolean {
            if (typeId == 101L) {
                filterMaybe()
            }
            output.write(eventPayload)
            return true
        }

        override fun onChunkEnd(chunkIndex: Int, skipped: Boolean): Boolean {
            updateChunk()
            return true
        }

        private fun updateChunk() {
            val chunkSize = countingOutput.count - ChunkHeader.SIZE - absoluteChunkStartPos
            output.flush() // save the output, otherwise below update will be lost
            RandomAccessFile(outputFile, "rw").use {
                it.seek(absoluteChunkStartPos)
                lastHeader!!.toBuilder().size(chunkSize).build().apply {
                    logger.debug("Updating chunk to $this")
                    write(it)
                }
            }
        }

        private fun filterMaybe() {

        }
    }
}
