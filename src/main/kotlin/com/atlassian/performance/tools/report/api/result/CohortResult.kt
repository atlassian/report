package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.io.api.directories
import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.parser.MergingActionMetricsParser
import com.atlassian.performance.tools.report.api.Timeline
import com.atlassian.performance.tools.report.api.parser.MergingNodeCountParser
import com.atlassian.performance.tools.report.api.parser.SystemMetricsParser
import java.nio.file.Path

@Deprecated(message = "Use RawCohortResult instead. For easier migration use toRawCohort method.")
interface CohortResult {

    fun prepareForJudgement(
        timeline: Timeline
    ): EdibleResult

    companion object {
        fun toRawCohortResult(cohortResult: CohortResult): RawCohortResult =
            when (cohortResult) {
                is FailedCohortResult -> cohortResult.convert()
                is FullCohortResult -> cohortResult.convert()
                else -> throw Exception("Only FailedCohortResult and FullCohortResult are supported!")
            }
    }
}

@Deprecated(message = "Use RawCohortResult.Factory instead.")
class FailedCohortResult(
    private val cohort: String,
    private val failure: Exception
) : CohortResult {

    override fun prepareForJudgement(
        timeline: Timeline
    ): EdibleResult = EdibleResult.Builder(cohort)
        .failure(failure)
        .build()

    internal fun convert(): RawCohortResult = RawCohortResult.Factory().legacyResult(cohort, failure)
}

@Deprecated(message = "Use RawCohortResult.Factory instead.")
class FullCohortResult(
    private val cohort: String,
    private val results: Path,
    private val actionParser: MergingActionMetricsParser,
    private val systemParser: SystemMetricsParser,
    private val nodeParser: MergingNodeCountParser
) : CohortResult {

    override fun prepareForJudgement(
        timeline: Timeline
    ): EdibleResult = EdibleResult.Builder(
        cohort
    )
        .actionMetrics(timeline.crop(parseActions(actionParser)))
        .systemMetrics(systemParser.parse(results))
        .nodeDistribution(nodeParser.parse(results))
        .build()

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

    internal fun convert(): RawCohortResult = RawCohortResult.Factory().fullResult(cohort, results)
}