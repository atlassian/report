package com.atlassian.performance.tools.report.api.jfr

import com.atlassian.performance.tools.report.jfr.FilteringJfrWriter
import jdk.jfr.consumer.RecordedEvent
import org.apache.logging.log4j.LogManager
import org.openjdk.jmc.flightrecorder.testutils.parser.StreamingChunkParser
import java.io.File
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

class JfrFilter private constructor(
    private val eventFilter: Predicate<RecordedEvent>,
    private val filteredRecording: Function<Path, Path>,
    private val symbolModifier: Consumer<MutableJvmSymbol>
) {
    private val logger = LogManager.getLogger(this::class.java)

    fun filter(recording: Path): File {
        val filteredRecording = filteredRecording.apply(recording).toFile()
        logger.debug("Writing filtered recording to $filteredRecording ...")
        filteredRecording.outputStream().buffered().use { outputStream ->
            val writer = FilteringJfrWriter(filteredRecording, outputStream, eventFilter)
            val parser = StreamingChunkParser(writer, symbolModifier::accept)
            parser.parse(recording)
        }
        return filteredRecording
    }

    class Builder {
        private var eventFilter: Predicate<RecordedEvent> = Predicate { true }
        private var filteredRecording: Function<Path, Path> =
            Function { it.resolveSibling("filtered-" + it.fileName.toString()) }
        private var symbolModifier: Consumer<MutableJvmSymbol> = Consumer { }

        fun eventFilter(eventFilter: Predicate<RecordedEvent>): Builder {
            this.eventFilter = eventFilter
            return this
        }

        fun symbolModifier(symbolModifier: Consumer<MutableJvmSymbol>) = apply {
            this.symbolModifier = symbolModifier
        }

        fun filteredRecording(filteredRecording: Function<Path, Path>): Builder {
            this.filteredRecording = filteredRecording
            return this
        }

        fun build(): JfrFilter {
            return JfrFilter(eventFilter, filteredRecording, symbolModifier)
        }
    }

}
