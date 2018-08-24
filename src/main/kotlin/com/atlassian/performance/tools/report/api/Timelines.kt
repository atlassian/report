package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.infrastructure.virtualusers.LoadProfile
import com.atlassian.performance.tools.jiraactions.ActionMetric
import java.time.Duration

class CompositeTimeline(
    private vararg val timelines: Timeline
) : Timeline {
    override fun crop(
        metrics: List<ActionMetric>
    ): List<ActionMetric> {
        var result: List<ActionMetric> = metrics
        for (timeline in timelines) {
            result = timeline.crop(result)
        }

        return result
    }
}

class ColdCachesTimeline(
    private val coldCaches: Duration = Duration.ofSeconds(30)
) : Timeline {
    override fun crop(
        metrics: List<ActionMetric>
    ): List<ActionMetric> {
        val timeline = metrics.sortedBy { it.start }
        val start = timeline.first().start
        val startEdge = start + coldCaches

        return timeline
            .asSequence()
            .filter { it.start > startEdge }
            .toList()
    }
}

class TestExecutionTimeline(
    private val testDuration: Duration
) : Timeline {
    override fun crop(
        metrics: List<ActionMetric>
    ): List<ActionMetric> {
        val timeline = metrics.sortedBy { it.start }
        val start = timeline.first().start
        val endEdge = start + testDuration
        return timeline
            .asSequence()
            .filter { it.start < endEdge }
            .toList()
    }
}

class StandardTimeline(
    loadProfile: LoadProfile
) : Timeline by CompositeTimeline(
    ColdCachesTimeline(),
    TestExecutionTimeline(loadProfile.loadSchedule.duration)
)

class FullTimeline : Timeline {
    override fun crop(
        metrics: List<ActionMetric>
    ): List<ActionMetric> = metrics
}

interface Timeline {
    fun crop(
        metrics: List<ActionMetric>
    ): List<ActionMetric>
}