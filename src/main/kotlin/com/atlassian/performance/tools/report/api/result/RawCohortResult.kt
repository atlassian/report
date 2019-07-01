package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.io.api.directories
import com.atlassian.performance.tools.jiraactions.api.ActionMetric
import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.parser.MergingActionMetricsParser
import com.atlassian.performance.tools.report.api.OutlierTrimming
import com.atlassian.performance.tools.report.api.Timeline
import com.atlassian.performance.tools.report.api.parser.MergingNodeCountParser
import com.atlassian.performance.tools.report.api.parser.SystemMetricsParser
import java.nio.file.Path

/**
 * Represent a single test cohort raw performance results.
 * @since 3.1.0
 */
abstract class RawCohortResult private constructor() {
    /**
     * An absolute path pointing to results on machine where the tests were executed.
     */
    abstract val results: Path

    /**
     * A failure that could have occurred during the test.
     */
    abstract val failure: Exception?

    /**
     * Prepares post-processed performance results.
     */
    abstract fun prepareForJudgement(
        timeline: Timeline
    ): EdibleResult

    /**
     * Prepares post-processed performance results.
     */
    abstract fun prepareForJudgement(
        timeline: Timeline,
        actionTypeToOutlierTrimming: Map<ActionType<*>, OutlierTrimming>
    ): EdibleResult

    class Factory {
        fun fullResult(
            cohort: String,
            results: Path
        ): RawCohortResult = FullRawCohortResult(cohort, results)

        fun failedResult(
            cohort: String,
            results: Path,
            failure: Exception
        ): RawCohortResult = FailedRawCohortResult(cohort, results, failure)

        @Deprecated("Only for internal use to keep compatibility.")
        internal fun legacyResult(
            cohort: String,
            failure: Exception
        ): RawCohortResult = LegacyRawCohortResult(cohort, failure)
    }

    private class FullRawCohortResult(
        private val cohort: String,
        override val results: Path
    ) : RawCohortResult() {
        override val failure: Exception? = null

        private val actionParser = MergingActionMetricsParser()
        private val systemParser = SystemMetricsParser()
        private val nodeParser = MergingNodeCountParser()

        override fun prepareForJudgement(timeline: Timeline): EdibleResult {
            return EdibleResult.Builder(cohort)
                .actionMetrics(timeline.crop(parseActions(actionParser)))
                .systemMetrics(systemParser.parse(results))
                .nodeDistribution(nodeParser.parse(results))
                .build()
        }

        override fun prepareForJudgement(timeline: Timeline, actionTypeToOutlierTrimming: Map<ActionType<*>, OutlierTrimming>): EdibleResult {
            return EdibleResult.Builder(cohort)
                .actionMetrics(timeline.crop(parseActions(actionParser)))
                .systemMetrics(systemParser.parse(results))
                .nodeDistribution(nodeParser.parse(results))
                .trimmingPerType(actionTypeToOutlierTrimming)
                .build()
        }

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

    private class FailedRawCohortResult(
        private val cohort: String,
        override val results: Path,
        override val failure: Exception
    ) : RawCohortResult() {

        override fun prepareForJudgement(timeline: Timeline): EdibleResult {
            return EdibleResult.Builder(cohort)
                .failure(failure)
                .build()
        }

        override fun prepareForJudgement(timeline: Timeline, actionTypeToOutlierTrimming: Map<ActionType<*>, OutlierTrimming>): EdibleResult {
            return EdibleResult.Builder(cohort)
                .failure(failure)
                .build()
        }
    }

    private class LegacyRawCohortResult(
        private val cohort: String,
        override val failure: Exception
    ) : RawCohortResult() {

        override val results: Path
            get() = throw Exception("Legacy failed results don't point to partial results on disk")

        override fun prepareForJudgement(timeline: Timeline): EdibleResult {
            return EdibleResult.Builder(cohort)
                .failure(failure)
                .build()
        }

        override fun prepareForJudgement(timeline: Timeline, actionTypeToOutlierTrimming: Map<ActionType<*>, OutlierTrimming>): EdibleResult {
            return EdibleResult.Builder(cohort)
                .failure(failure)
                .build()
        }
    }
}
