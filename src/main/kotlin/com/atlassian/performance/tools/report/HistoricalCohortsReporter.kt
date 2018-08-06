package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.infrastructure.virtualusers.GrowingLoadSchedule
import com.atlassian.performance.tools.infrastructure.virtualusers.LoadProfile
import com.atlassian.performance.tools.jiraactions.*
import com.atlassian.performance.tools.workspace.RootWorkspace
import com.atlassian.performance.tools.workspace.TaskWorkspace
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import java.time.Duration

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

    private val fullCriteria = PerformanceCriteria(
        actionCriteria = actionTypes.map {
            it to Criteria(
                toleranceRatio = Float.NaN,
                minimumSampleSize = 0,
                acceptableErrorCount = Int.MAX_VALUE
            )
        }.toMap(),
        loadProfile = LoadProfile(
            loadSchedule = GrowingLoadSchedule(
                duration = Duration.ofSeconds(0),
                initialNodes = 0,
                finalNodes = 0
            ),
            virtualUsersPerNode = 0,
            seed = 0L
        ),
        nodes = 0
    )

    fun report(
        report: Path
    ) {
        DataReporter(
            output = report.toFile(),
            labels = actionTypes.map { it.label }
        ).report(
            workspace
                .listPreviousTasks()
                .mapNotNull { extractResults(it) }
                .map { it.actionStats }
        )
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
        ).prepareForJudgement(fullCriteria, FullTimeline())
        logger.info("Found previous results in ${task.directory}")
        edibleResult
    } catch (e: Exception) {
        logger.debug("${task.directory} has no results", e)
        null
    }
}