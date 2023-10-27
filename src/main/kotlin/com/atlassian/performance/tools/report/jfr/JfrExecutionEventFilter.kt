package com.atlassian.performance.tools.report.jfr

import org.openjdk.jmc.flightrecorder.testutils.parser.*
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader.MAGIC
import java.io.*
import java.nio.file.Path


class JfrExecutionEventFilter {

    fun go(recording: Path): File {
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
        private var chunkHeaderSize = 0L

        override fun onChunkStart(chunkIndex: Int, header: ChunkHeader): Boolean {
            lastHeader = header
            absoluteChunkStartPos = countingOutput.count
            writeChunkHeader(header, output)
            chunkHeaderSize = countingOutput.count - absoluteChunkStartPos
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

        override fun onEvent(typeId: Long, stream: RecordingStream, payloadSize: Long): Boolean {
            if (typeId == 101L) {
                filterMaybe()
            }

            ByteArray(payloadSize.toInt()).let { eventPayload ->
                stream.read(eventPayload, 0, payloadSize.toInt())
                output.write(eventPayload)
            }
            return true
        }

        override fun onChunkEnd(chunkIndex: Int, skipped: Boolean): Boolean {
            updateChunkSize()
            return true
        }

        private fun writeChunkHeader(header: ChunkHeader, output: DataOutput) {
            with(header) {
                output.write(MAGIC)
                output.writeShort(major.toInt())
                output.writeShort(minor.toInt())
                output.writeLong(size)
                output.writeLong(cpOffset)
                output.writeLong(metaOffset)
                output.writeLong(startNanos)
                output.writeLong(duration)
                output.writeLong(startTicks)
                output.writeLong(frequency)
                output.writeInt(if (compressed) 1 else 0)
                //   130357145
                // - 167235804
                //    36878656
            }

        }

        private fun updateChunkSize() {
            val chunkSize = countingOutput.count - chunkHeaderSize - absoluteChunkStartPos
            RandomAccessFile(outputFile, "rw").use {
                it.seek(absoluteChunkStartPos)
                writeChunkHeader(
                    lastHeader!!.toBuilder()
                        .size(chunkSize)
                        .build(), it
                )
            }
        }

        private fun filterMaybe() {

        }
    }
}
