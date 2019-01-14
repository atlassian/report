package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.jiraactions.api.parser.MergingActionMetricsParser
import com.atlassian.performance.tools.report.api.parser.MergingNodeCountParser
import com.atlassian.performance.tools.report.api.parser.SystemMetricsParser
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.api.result.FullCohortResult
import com.atlassian.performance.tools.report.api.result.InteractionStats
import com.atlassian.performance.tools.report.chart.MeanLatencyChart
import com.atlassian.performance.tools.workspace.api.RootWorkspace
import com.atlassian.performance.tools.workspace.api.TaskWorkspace
import org.apache.logging.log4j.LogManager
import java.nio.file.Path

/**
 * Reports on cohort results from all JPT tasks available in the [workspace].
 */
class HistoricalCohortsReporter(
    private val workspace: RootWorkspace
) {
    private val logger = LogManager.getLogger(this::class.java)

    private val actionTypes = listOf(
        VIEW_BOARD,
        VIEW_ISSUE,
        VIEW_DASHBOARD,
        SEARCH_WITH_JQL,
        ADD_COMMENT_SUBMIT,
        CREATE_ISSUE_SUBMIT,
        EDIT_ISSUE_SUBMIT,
        PROJECT_SUMMARY,
        BROWSE_PROJECTS,
        BROWSE_BOARDS
    )

    fun dump() {
        val stats = getStats()
        val labels = actionTypes.map { it.label }
        val report = workspace.directory.resolve("results.csv")
        DataReporter(
            output = report.toFile(),
            labels = labels
        ).report(stats)

        val chart = workspace.directory.resolve("mean-latency-chart.html")
        MeanLatencyChart().plot(
            stats = stats,
            labels = labels,
            output = chart.toFile()
        )
    }

    private fun getStats(): Collection<InteractionStats> {
        return workspace
            .listTasks()
            .mapNotNull { extractResults(it) }
            .map { it.actionStats }
    }

    private fun extractResults(
        task: TaskWorkspace
    ): EdibleResult? = try {
        val edibleResult = FullCohortResult(
            cohort = task.directory.fileName.toString(),
            results = task.directory,
            actionParser = MergingActionMetricsParser(),
            systemParser = SystemMetricsParser(),
            nodeParser = MergingNodeCountParser()
        ).prepareForJudgement(FullTimeline())
        logger.info("Found previous results in ${task.directory}")
        edibleResult
    } catch (e: Exception) {
        logger.debug("${task.directory} has no results", e)
        null
    }
}