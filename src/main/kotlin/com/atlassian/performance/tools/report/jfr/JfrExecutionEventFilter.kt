package com.atlassian.performance.tools.report.jfr

import org.openjdk.jmc.flightrecorder.testutils.parser.*
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader.MAGIC
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Path
import java.util.function.Consumer


class JfrExecutionEventFilter {

    fun go(recording: Path): File {
        recording.toFile().inputStream().buffered().use { inputStream ->
            val filteredRecording = recording.resolveSibling("filtered-" + recording.fileName.toString()).toFile()
            filteredRecording.outputStream().buffered().use { outputStream ->
                val writer = FilteringJfrWriter(DataOutputStream(outputStream))
                val parser = StreamingChunkParser()
                parser.parse(inputStream, writer)
            }
            return filteredRecording
        }
    }

    class FilteringJfrWriter(
        val output: DataOutputStream
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

        override fun onMetadata(metadata: MetadataEvent, writes: Consumer<DataOutputStream>): Boolean {
            writes.accept(output) // it's working!
            // TODO check also transfer from start to end position (metadata.positionBeforeRead to metadata.positionAfterRead). It might require re-reading the file
            return true
        }

        override fun onEvent(typeId: Long, stream: RecordingStream, payloadSize: Long, eventSize: Long): Boolean {
            // TODO there are 4 bytes missing at the beginning of the event
            if (typeId == 101L) {
                filterMaybe()
            }

            VarInt.write(eventSize, output)
            VarInt.write(typeId, output)

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
