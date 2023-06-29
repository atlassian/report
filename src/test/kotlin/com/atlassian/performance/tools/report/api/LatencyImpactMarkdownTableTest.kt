package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.jirasoftwareactions.api.actions.WorkOnBacklog.Companion.VIEW_BACKLOG
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
            Builder(BROWSE_BOARDS, -0.0091, ofMillis(-3)).noise(true).build(),
            Builder(VIEW_BOARD, -0.0156, ofMillis(-10)).noise(true).build(),
            Builder(VIEW_BACKLOG, -0.0121, ofMillis(-7)).noise(true).build(),
            Builder(CREATE_ISSUE_SUBMIT, -0.0308, ofMillis(-14)).signal(true).build(),
            Builder(EDIT_ISSUE_SUBMIT, -0.0206, ofMillis(-13)).signal(true).build(),
            Builder(ADD_COMMENT_SUBMIT, -0.0359, ofMillis(-23)).signal(true).build(),
            Builder(CREATE_ISSUE, -0.1781, ofMillis(-711)).signal(true).build(),
            Builder(EDIT_ISSUE, -0.1460, ofMillis(-263)).signal(true).build(),
            Builder(ADD_COMMENT, -0.1393, ofMillis(-165)).signal(true).build(),
            Builder(SEARCH_JQL_SIMPLE, -0.1152, ofMillis(-124)).signal(true).build(),
            Builder(VIEW_ISSUE, -0.0099, ofMillis(-5)).noise(true).build(),
            Builder(VIEW_DASHBOARD, +0.0367, ofMillis(+11)).signal(true).build(),
            Builder(SEARCH_JQL_CHANGELOG, -0.0238, ofMillis(-48)).signal(true).build(),
            Builder(PROJECT_SUMMARY, -0.0204, ofMillis(-5)).signal(true).build(),
            Builder(BROWSE_PROJECTS, -0.0156, ofMillis(-6)).noise(true).build(),
            Builder(ActionType("Switch issue nav view") { }, -0.0633, ofMillis(-8)).signal(true).build()
        )
        val workspace = TestWorkspace(createTempDirectory(javaClass.simpleName))
        val table = LatencyImpactMarkdownTable(workspace)

        // when
        impacts.forEach { table.accept(it) }

        // then
        assertThat(workspace.directory.resolve("latency-impact-table.md")).hasContent(
            """
            | Action                | Latency impact | Latency impact | Classification |
            |-----------------------|----------------|----------------|----------------|
            | Browse Boards         | -0.91 %        | -3 ms          | NOISE          |
            | View Board            | -1.56 %        | -10 ms         | NOISE          |
            | View Backlog          | -1.21 %        | -7 ms          | NOISE          |
            | Create Issue          | -3.08 %        | -14 ms         | IMPROVEMENT    |
            | Edit Issue            | -2.06 %        | -13 ms         | IMPROVEMENT    |
            | Add Comment           | -3.59 %        | -23 ms         | IMPROVEMENT    |
            | Full Create Issue     | -17.81 %       | -711 ms        | IMPROVEMENT    |
            | Full Edit Issue       | -14.60 %       | -263 ms        | IMPROVEMENT    |
            | Full Add Comment      | -13.93 %       | -165 ms        | IMPROVEMENT    |
            | Simple searches       | -11.52 %       | -124 ms        | IMPROVEMENT    |
            | View Issue            | -0.99 %        | -5 ms          | NOISE          |
            | View Dashboard        | +3.67 %        | +11 ms         | REGRESSION     |
            | Changelog searches    | -2.38 %        | -48 ms         | IMPROVEMENT    |
            | Project Summary       | -2.04 %        | -5 ms          | IMPROVEMENT    |
            | Browse Projects       | -1.56 %        | -6 ms          | NOISE          |
            | Switch issue nav view | -6.33 %        | -8 ms          | IMPROVEMENT    |
            """.trimIndent()
        )
    }
}
