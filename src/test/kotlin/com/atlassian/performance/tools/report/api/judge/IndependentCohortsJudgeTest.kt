package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.FullTimeline
import com.atlassian.performance.tools.report.api.PerformanceCriteria
import com.atlassian.performance.tools.report.api.result.FailedCohortResult
import com.atlassian.performance.tools.report.api.result.LocalRealResult
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class IndependentCohortsJudgeTest {

    @Test
    fun shouldJudgeWithoutActionCriteria() {
        val results = listOf(
                Paths.get("JIRA-JPT760-JOB1-8/alpha"),
                Paths.get("JIRA-JPT760-JOB1-8/beta")
        ).map { LocalRealResult(it).loadEdible() }
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
            LocalRealResult(resultPath).loadEdible()
        }.plus(
                FailedCohortResult(
                        cohort = "a-failed-cohort",
                        failure = RuntimeException("Provisioning failed")
                ).prepareForJudgement(FullTimeline())
        )
        val junitReports = Files.createTempDirectory("junit-reports")

        val exception: Exception? = try {
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
            null
        } catch (e: Exception) {
            e
        }

        assertThat(exception)
                .`as`("result failure")
                .hasMessageContaining("java.lang.RuntimeException: Provisioning failed")
    }
}