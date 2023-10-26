package com.atlassian.performance.tools.report.jfr

import org.openjdk.jmc.flightrecorder.testutils.parser.*
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader.MAGIC
import java.io.DataOutputStream
import java.nio.file.Path


class JfrExecutionEventFilter {

    fun go(recording: Path) {
        recording.toFile().inputStream().buffered().use { inputStream ->
            val filteredRecording = recording.resolveSibling("filtered-" + recording.fileName.toString())
            filteredRecording.toFile().outputStream().buffered().use { outputStream ->
                val writer = FilteringJfrWriter(DataOutputStream(outputStream))
                val parser = StreamingChunkParser()
                parser.parse(inputStream, writer)
            }
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

        override fun onMetadata(metadata: MetadataEvent): Boolean {
//            with(metadata) {
//
//            }
            return true
        }

        override fun onEvent(typeId: Long, stream: RecordingStream, payloadSize: Long): Boolean {
            if (typeId == 101L) {
                filterMaybe()
            }

            while (stream.available() > 0) {
                output.write(stream.readInt())
            }
            return true
        }

        private fun filterMaybe() {

        }
    }
}
