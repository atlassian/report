package com.atlassian.performance.tools.report.jfr

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

    fun filter(recording: Path): File {
        recording.toFile().inputStream().buffered().use { inputStream ->
            val filteredRecording = recording.resolveSibling("filtered-" + recording.fileName.toString()).toFile()
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
            output.writeLong(eventSize)
        }

        override fun onEventType(eventType: Long) {
            VarInt.write(eventType, output)
        }

        override fun onMetadata(metadata: MetadataEvent): Boolean {
            val payloadSize = metadata.positionAfterRead - metadata.positionBeforeRead
            input.toFile().inputStream().use { inputStream ->
                inputStream.skip(metadata.positionBeforeRead)
                val eventPayload = ByteArray(payloadSize.toInt())
                inputStream.read(eventPayload)
                output.write(eventPayload)
            }
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
            updateChunkSize()
            return true
        }

        private fun updateChunkSize() {
            val chunkSize = countingOutput.count - ChunkHeader.SIZE - absoluteChunkStartPos
            RandomAccessFile(outputFile, "rw").use {
                it.seek(absoluteChunkStartPos)
                lastHeader!!.toBuilder().size(chunkSize).build()
                    .write(it)
            }
        }

        private fun filterMaybe() {

        }
    }
}
