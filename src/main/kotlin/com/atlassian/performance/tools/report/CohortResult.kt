package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.io.directories
import com.atlassian.performance.tools.jiraactions.ActionMetric
import com.atlassian.performance.tools.jiraactions.MergingActionMetricsParser
import java.nio.file.Path

interface CohortResult {

    fun prepareForJudgement(
        criteria: PerformanceCriteria,
        timeline: Timeline
    ): EdibleResult
}

class FailedCohortResult(
    private val cohort: String,
    private val failure: Exception
) : CohortResult {

    override fun prepareForJudgement(
        criteria: PerformanceCriteria,
        timeline: Timeline
    ): EdibleResult = EdibleResult(
        failure = failure,
        criteria = criteria,
        allActionMetrics = emptyList(),
        cohort = cohort,
        systemMetrics = emptyList(),
        nodeDistribution = emptyMap()
    )
}

class FullCohortResult(
    private val cohort: String,
    private val results: Path,
    private val actionParser: MergingActionMetricsParser,
    private val systemParser: SystemMetricsParser,
    private val nodeParser: MergingNodeCountParser
) : CohortResult {

    override fun prepareForJudgement(
        criteria: PerformanceCriteria,
        timeline: Timeline
    ): EdibleResult = EdibleResult(
        criteria = criteria,
        allActionMetrics = timeline.crop(parseActions(actionParser)),
        cohort = cohort,
        systemMetrics = systemParser.parse(results),
        nodeDistribution = nodeParser.parse(results),
        failure = null
    )

    private fun parseActions(
        parser: MergingActionMetricsParser
    ): List<ActionMetric> {
        val nodeDirectories = results
            .resolve("virtual-users")
            .toFile()
            .directories()
        val vuDirectories = nodeDirectories
            .map { it.resolve("test-results") }
            .flatMap { it.directories() }
            .map { it.resolve("action-metrics.jpt") }
        return parser.parse(vuDirectories)
    }
}