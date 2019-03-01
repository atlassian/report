package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.io.api.ensureDirectory
import com.atlassian.performance.tools.jiraactions.api.parser.MergingActionMetricsParser
import com.atlassian.performance.tools.report.api.FullTimeline
import com.atlassian.performance.tools.report.api.parser.MergingNodeCountParser
import com.atlassian.performance.tools.report.api.parser.SystemMetricsParser
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

class LocalRealResult(
    private val relativeResourcePath: Path
) {
    fun load(): CohortResult = FullCohortResult(
        cohort = relativeResourcePath.joinToString(separator = "/") { it.toString() },
        results = File(
            this::class
                .java
                .getResource("real-results/$relativeResourcePath")
                .toURI()
        ).toPath(),
        actionParser = MergingActionMetricsParser(),
        systemParser = SystemMetricsParser(),
        nodeParser = MergingNodeCountParser()
    )

    fun loadRaw(): RawCohortResult = RawCohortResult.Factory().fullResult(
        cohort = relativeResourcePath.joinToString(separator = "/") { it.toString() },
        results = extractDirectory()
    )

    private fun extractDirectory(): Path {
        val file = File(
            this::class
                .java
                .getResource("real-results/$relativeResourcePath")
                .toURI()
        )
        return if (file.name.endsWith(".zip")) {
            unzip(file)
        } else {
            file.toPath()
        }
    }

    private fun unzip(
        file: File
    ): Path {
        val unpacked = Files.createTempDirectory("apt-report-test-result")
        val zip = ZipFile(file)
        zip.stream().forEach { entry ->
            val unpackedEntry = unpacked.resolve(entry.name)
            if (entry.isDirectory) {
                unpackedEntry.ensureDirectory()
            } else {
                zip.getInputStream(entry).use { packedStream ->
                    unpackedEntry.toFile().outputStream().use { unpackedStream ->
                        packedStream.copyTo(unpackedStream)
                    }
                }
            }
        }
        return unpacked
    }

    fun loadEdible(): EdibleResult = loadRaw().prepareForJudgement(FullTimeline())
}
