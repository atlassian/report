package com.atlassian.performance.tools.report.jfr

import org.openjdk.jmc.flightrecorder.testutils.parser.*
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader.MAGIC
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Path


class JfrExecutionEventFilter {

    fun go(recording: Path): File {
        recording.toFile().inputStream().buffered().use { inputStream ->
            val filteredRecording = recording.resolveSibling("filtered-" + recording.fileName.toString()).toFile()
            filteredRecording.outputStream().buffered().use { outputStream ->
                val writer = FilteringJfrWriter(DataOutputStream(outputStream), recording)
                val parser = StreamingChunkParser()
                parser.parse(inputStream, writer)
            }
            return filteredRecording
        }
    }

    class FilteringJfrWriter(
        val output: DataOutputStream,
        val input: Path
    ) : ChunkParserListener {

        override fun onChunkStart(chunkIndex: Int, header: ChunkHeader): Boolean {
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
            }
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

            if (typeId != checkpointEventType) {
                ByteArray(payloadSize.toInt()).let { eventPayload ->
                    stream.read(eventPayload, 0, payloadSize.toInt())
                    output.write(eventPayload)
                }
            }
            return true
        }

        private fun filterMaybe() {

        }
    }
}
