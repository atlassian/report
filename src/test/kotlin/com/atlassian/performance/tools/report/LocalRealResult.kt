package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.infrastructure.virtualusers.GrowingLoadSchedule
import com.atlassian.performance.tools.infrastructure.virtualusers.LoadProfile
import com.atlassian.performance.tools.jiraactions.MergingActionMetricsParser
import java.io.File
import java.nio.file.Path
import java.time.Duration

class LocalRealResult(
    private val path: Path
) {

    private val pointlessCriteria = PerformanceCriteria(
        actionCriteria = emptyMap(),
        loadProfile = LoadProfile(
            virtualUsersPerNode = 2398472,
            loadSchedule = GrowingLoadSchedule(
                duration = Duration.ZERO,
                finalNodes = 4783
            ),
            seed = 22222222
        )
    )

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

    fun loadEdible(): EdibleResult = loadRaw().prepareForJudgement(pointlessCriteria, FullTimeline())
}