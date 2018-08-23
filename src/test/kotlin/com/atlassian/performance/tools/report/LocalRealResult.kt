package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.MergingActionMetricsParser
import java.io.File
import java.nio.file.Path

class LocalRealResult(
    private val path: Path
) {

    fun loadRaw(): CohortResult = FullCohortResult(
        cohort = path.toString(),
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

    fun loadEdible(): EdibleResult = loadRaw().prepareForJudgement(FullTimeline())
}