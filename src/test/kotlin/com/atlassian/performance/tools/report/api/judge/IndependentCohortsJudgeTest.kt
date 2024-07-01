package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.BROWSE_PROJECTS
import com.atlassian.performance.tools.jiraactions.api.VIEW_DASHBOARD
import com.atlassian.performance.tools.jiraactions.api.VIEW_ISSUE
import com.atlassian.performance.tools.report.api.Criteria
import com.atlassian.performance.tools.report.api.FullTimeline
import com.atlassian.performance.tools.report.api.PerformanceCriteria
import com.atlassian.performance.tools.report.api.result.LocalRealResult
import com.atlassian.performance.tools.report.api.result.RawCohortResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Paths

class IndependentCohortsJudgeTest {
    @Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    @Test
    fun shouldJudgeWithoutActionCriteria() {
        val results = listOf(
            Paths.get("JIRA-JPT760-JOB1-8/alpha"),
            Paths.get("JIRA-JPT760-JOB1-8/beta")
        ).map { LocalRealResult(it).loadEdible(tempFolder) }
        val junitReports = Files.createTempDirectory("junit-reports")

        val verdict = IndependentCohortsJudge().judge(
            results = results,
            workspace = TestWorkspace(Files.createTempDirectory("icj-workspace")),
            criteria = PerformanceCriteria(
                actionCriteria = emptyMap(),
                virtualUserLoad = VirtualUserLoad()
            )
        )

        verdict.assertAccepted(
            testClassName = "icj-test",
            testResults = junitReports,
            expectedReportCount = 6
        )
    }

    @Test
    fun shouldJudgeFailure() {
        val results = listOf(
            Paths.get("JIRA-JPT760-JOB1-8/alpha"),
            Paths.get("JIRA-JPT760-JOB1-8/beta")
        ).map { resultPath ->
            LocalRealResult(resultPath).loadEdible(tempFolder)
        }.plus(
            RawCohortResult.Factory().failedResult(
                cohort = "a-failed-cohort",
                results = Files.createTempDirectory("a-failed-cohort"),
                failure = RuntimeException("Provisioning failed")
            ).prepareForJudgement(FullTimeline())
        )
        val junitReports = Files.createTempDirectory("junit-reports")


        assertThatThrownBy {
            IndependentCohortsJudge().judge(
                results = results,
                workspace = TestWorkspace(Files.createTempDirectory("icj-workspace")),
                criteria = PerformanceCriteria(
                    actionCriteria = emptyMap(),
                    virtualUserLoad = VirtualUserLoad()
                )
            ).assertAccepted(
                testClassName = "icj-test",
                testResults = junitReports,
                expectedReportCount = 7
            )
        }.`as`("result failure")
            .hasMessageContaining("java.lang.RuntimeException: Provisioning failed")
    }

    @Test
    fun shouldPreserveColumnsOrderInReport() {
        val results = listOf(
            Paths.get("JIRA-JPT760-JOB1-8/alpha"),
            Paths.get("JIRA-JPT760-JOB1-8/beta")
        ).map { LocalRealResult(it).loadEdible(tempFolder) }
        val criteria = Criteria(minimumSampleSize = 0)
        val actionCriteria = mapOf<ActionType<*>, Criteria>(
            VIEW_ISSUE to criteria,
            BROWSE_PROJECTS to criteria,
            VIEW_DASHBOARD to criteria
        )
        val workspace = TestWorkspace(Files.createTempDirectory("icj-workspace"))
        val report = workspace.directory.resolve("summary-per-cohort.csv").toFile()
        val expectedHeader = "cohort,metric,View Issue,Browse Projects,View Dashboard,AGGREGATE"

        IndependentCohortsJudge().judge(
            results = results,
            workspace = workspace,
            criteria = PerformanceCriteria(
                actionCriteria = actionCriteria,
                virtualUserLoad = VirtualUserLoad()
            )
        )

        val actualHeader = report.reader().readLines()[0]
        assertEquals(expectedHeader, actualHeader)
    }
}
