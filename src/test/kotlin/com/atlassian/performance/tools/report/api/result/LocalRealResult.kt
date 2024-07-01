package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.jiraactions.api.parser.MergingActionMetricsParser
import com.atlassian.performance.tools.report.api.FullTimeline
import com.atlassian.performance.tools.report.api.parser.MergingNodeCountParser
import com.atlassian.performance.tools.report.api.parser.SystemMetricsParser
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Path

class LocalRealResult(
    private val relativeResourcePath: Path
) {
    @Deprecated("Use loadRaw")
    fun load(): CohortResult = FullCohortResult(
        cohort = relativeResourcePath.joinToString(separator = "/") { it.toString() },
        results = File(
            this::class
                .java
                .getResource("real-results/$relativeResourcePath")!!
                .toURI()
        ).toPath(),
        actionParser = MergingActionMetricsParser(),
        systemParser = SystemMetricsParser(),
        nodeParser = MergingNodeCountParser()
    )

    fun loadRaw(tempFolder: TemporaryFolder): RawCohortResult = RawCohortResult.Factory().fullResult(
        cohort = relativeResourcePath.joinToString(separator = "/") { it.toString() },
        results = CompressedResult(relativeResourcePath).extractDirectory(tempFolder)
    )

    fun loadEdible(tempFolder: TemporaryFolder): EdibleResult = loadRaw(tempFolder).prepareForJudgement(FullTimeline())
}
