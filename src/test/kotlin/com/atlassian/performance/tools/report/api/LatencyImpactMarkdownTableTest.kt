package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.jirasoftwareactions.api.actions.WorkOnBacklog.Companion.VIEW_BACKLOG
import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import com.atlassian.performance.tools.report.api.judge.LatencyImpact.Builder
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.file.Files.createTempDirectory
import java.time.Duration.ofMillis

class LatencyImpactMarkdownTableTest {

    /**
     * Equivalent of [an internal report](https://hello.atlassian.net/l/cp/jCtvREHf).
     */
    @Test
    fun shouldRenderReportFromApril2023() {
        // given
        val impacts = listOf(
            Builder(BROWSE_BOARDS, -0.0091, ofMillis(-3)).irrelevant().build(),
            Builder(VIEW_BOARD, -0.0156, ofMillis(-10)).irrelevant().build(),
            Builder(VIEW_BACKLOG, -0.0121, ofMillis(-7)).irrelevant().build(),
            Builder(CREATE_ISSUE_SUBMIT, -0.0308, ofMillis(-14)).relevant().build(),
            Builder(EDIT_ISSUE_SUBMIT, -0.0206, ofMillis(-13)).relevant().build(),
            Builder(ADD_COMMENT_SUBMIT, -0.0359, ofMillis(-23)).relevant().build(),
            Builder(CREATE_ISSUE, -0.1781, ofMillis(-711)).relevant().build(),
            Builder(EDIT_ISSUE, -0.1460, ofMillis(-263)).relevant().build(),
            Builder(ADD_COMMENT, -0.1393, ofMillis(-165)).relevant().build(),
            Builder(SEARCH_JQL_SIMPLE, -0.1152, ofMillis(-124)).relevant().build(),
            Builder(VIEW_ISSUE, -0.0099, ofMillis(-5)).irrelevant().build(),
            Builder(VIEW_DASHBOARD, +0.0367, ofMillis(+11)).relevant().build(),
            Builder(SEARCH_JQL_CHANGELOG, -0.0238, ofMillis(-48)).relevant().build(),
            Builder(PROJECT_SUMMARY, -0.0204, ofMillis(-5)).relevant().build(),
            Builder(BROWSE_PROJECTS, -0.0156, ofMillis(-6)).irrelevant().build(),
            Builder(ActionType("Switch issue nav view") { }, -0.0633, ofMillis(-8)).relevant().build()
        )
        val workspace = TestWorkspace(createTempDirectory(javaClass.simpleName))
        val table = LatencyImpactMarkdownTable(workspace)

        // when
        impacts.forEach { table.accept(it) }

        // then
        assertThat(workspace.directory.resolve("latency-impact-table.md")).hasContent(
            """
            | Action                | Latency impact | Latency impact | Classification | Confidence |
            |-----------------------|----------------|----------------|----------------|------------|
            | Browse Boards         | -1 %           | -3 ms          | NO IMPACT      | 68.27 %    |
            | View Board            | -2 %           | -10 ms         | NO IMPACT      | 68.27 %    |
            | View Backlog          | -1 %           | -7 ms          | NO IMPACT      | 68.27 %    |
            | Create Issue          | -3 %           | -14 ms         | IMPROVEMENT    | 68.27 %    |
            | Edit Issue            | -2 %           | -13 ms         | IMPROVEMENT    | 68.27 %    |
            | Add Comment           | -4 %           | -23 ms         | IMPROVEMENT    | 68.27 %    |
            | Full Create Issue     | -18 %          | -711 ms        | IMPROVEMENT    | 68.27 %    |
            | Full Edit Issue       | -15 %          | -263 ms        | IMPROVEMENT    | 68.27 %    |
            | Full Add Comment      | -14 %          | -165 ms        | IMPROVEMENT    | 68.27 %    |
            | Simple searches       | -12 %          | -124 ms        | IMPROVEMENT    | 68.27 %    |
            | View Issue            | -1 %           | -5 ms          | NO IMPACT      | 68.27 %    |
            | View Dashboard        | +4 %           | +11 ms         | REGRESSION     | 68.27 %    |
            | Changelog searches    | -2 %           | -48 ms         | IMPROVEMENT    | 68.27 %    |
            | Project Summary       | -2 %           | -5 ms          | IMPROVEMENT    | 68.27 %    |
            | Browse Projects       | -2 %           | -6 ms          | NO IMPACT      | 68.27 %    |
            | Switch issue nav view | -6 %           | -8 ms          | IMPROVEMENT    | 68.27 %    |
            """.trimIndent()
        )
    }

    @Test
    fun shouldShowConfidenceForRepeatedTests() {
        // given
        val impacts = listOf(
            impacts(BROWSE_BOARDS, green = 4, red = 0),
            impacts(VIEW_BOARD, green = 1, red = 3),
            impacts(VIEW_BACKLOG, green = 5, red = 8),
            impacts(SEARCH_JQL_SIMPLE, green = 10, red = 2),
            impacts(ADD_COMMENT, green = 26, red = 14),
            impacts(BROWSE_PROJECTS, green = 10, red = 13),
            impacts(CREATE_ISSUE, green = 2, red = 2, grey = 1),
            impacts(VIEW_ISSUE, green = 3, red = 1, grey = 1),
            impacts(VIEW_DASHBOARD, green = 0, red = 0, grey = 5),
            impacts(PROJECT_SUMMARY, green = 1, red = 1, grey = 8)
        ).flatten()
        val workspace = TestWorkspace(createTempDirectory(javaClass.simpleName))
        val table = LatencyImpactMarkdownTable(workspace)

        // when
        impacts.forEach { table.accept(it) }

        // then
        assertThat(workspace.directory.resolve("latency-impact-table.md")).hasContent(
            """
            | Action                | Latency impact | Latency impact | Classification | Confidence |
            |-----------------------|----------------|----------------|----------------|------------|
            | Browse Boards         | -10 %          | -30 ms         | IMPROVEMENT    | 95.45 %    |
            | View Board            | -10 % to +20 % | -30 to +60 ms  | REGRESSION     | 68.27 %    |
            | View Backlog          | -10 % to +20 % | -30 to +60 ms  | REGRESSION     | 59.46 %    |
            | Simple searches       | -10 % to +20 % | -30 to +60 ms  | IMPROVEMENT    | 97.91 %    |
            | Full Add Comment      | -10 % to +20 % | -30 to +60 ms  | IMPROVEMENT    | 94.22 %    |
            | Browse Projects       | -10 % to +20 % | -30 to +60 ms  | REGRESSION     | 46.84 %    |
            | Full Create Issue     | -10 % to +20 % | -30 to +60 ms  | INCONCLUSIVE   | -          |
            | View Issue            | -10 % to +20 % | -30 to +60 ms  | IMPROVEMENT    | 34.53 %    |
            | View Dashboard        | +1 %           | +3 ms          | NO IMPACT      | 97.47 %    |
            | Project Summary       | -10 % to +20 % | -30 to +60 ms  | NO IMPACT      | 94.22 %    |
            """.trimIndent()
        )
    }

    private fun impacts(action: ActionType<*>, green: Int, red: Int, grey: Int = 0): List<LatencyImpact> {
        val greens = List(green) {
            Builder(action, -0.10, ofMillis(-30)).relevant().build()
        }
        val reds = List(red) {
            Builder(action, +0.20, ofMillis(+60)).relevant().build()
        }
        val greys = List(grey) {
            Builder(action, +0.01, ofMillis(+3)).irrelevant().build()
        }
        return greens + reds + greys
    }
}
