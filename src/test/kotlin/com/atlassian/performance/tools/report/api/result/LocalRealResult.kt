package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.jiraactions.api.parser.MergingActionMetricsParser
import com.atlassian.performance.tools.report.api.FullTimeline
import com.atlassian.performance.tools.report.api.parser.MergingNodeCountParser
import com.atlassian.performance.tools.report.api.parser.SystemMetricsParser
import java.io.File
import java.nio.file.Path

class LocalRealResult(
    private val path: Path
) {
    fun load(): CohortResult = FullCohortResult(
        cohort = path.joinToString(separator = "/") { it.toString() },
        results = File(
            this::class
                .java
                .getResource("real-results/$path")
                .toURI()
        ).toPath(),
        actionParser = MergingActionMetricsParser(),
        systemParser = SystemMetricsParser(),
        nodeParser = MergingNodeCountParser()
    )

    fun loadRaw(): RawCohortResult = RawCohortResult.Factory().fullResult(
        cohort = path.joinToString(separator = "/") { it.toString() },
        results = File(
            this::class
                .java
                .getResource("real-results/$path")
                .toURI()
        ).toPath()
    )

    fun loadEdible(): EdibleResult = loadRaw().prepareForJudgement(FullTimeline())
}