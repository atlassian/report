package com.atlassian.performance.tools.report.jfr

import org.openjdk.jmc.flightrecorder.testutils.parser.*
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader.MAGIC
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Path


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

        private fun writeStringTable(metadata: MetadataEvent) {
            val eventTypeNameMap = metadata.eventTypeNameMapBacking
            VarInt.write(eventTypeNameMap.size.toLong(), output)
            eventTypeNameMap.forEach { (id, name) ->
                VarInt.write(id, output)
                if (id == 3L) {
                    VarInt.write(name.length.toLong(), output)
                    output.writeBytes(name)
                }
                if (id == 4L) {
                    VarInt.write(name.length.toLong(), output)
                    for (character in name) {
                        VarInt.write(character.toLong(), output)
                    }
                }
            }
        }

        private fun writeElements(metadata: MetadataEvent) {

        }

        override fun onMetadata(metadata: MetadataEvent): Boolean {
            with(metadata) {
                VarInt.write(size.toLong(), output)
                VarInt.write(0, output)
                VarInt.write(startTime, output)
                VarInt.write(duration, output)
                VarInt.write(metadataId, output)
                writeStringTable(this)
                writeElements(this)
            }
            return true
        }

        override fun onEvent(typeId: Long, stream: RecordingStream, payloadSize: Long, eventSize: Long): Boolean {
            if (typeId == 101L) {
                filterMaybe()
            }

            VarInt.write(eventSize, output)
            VarInt.write(typeId, output)

            var i = 0
            while (i++ < payloadSize) {
                output.writeByte(stream.read().toInt())
            }
            return true
        }

        private fun filterMaybe() {

        }
    }
}
