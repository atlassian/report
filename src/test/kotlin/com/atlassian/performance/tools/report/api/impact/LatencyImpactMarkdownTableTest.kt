package com.atlassian.performance.tools.report.api.impact

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.jirasoftwareactions.api.actions.WorkOnBacklog.Companion.VIEW_BACKLOG
import com.atlassian.performance.tools.report.api.impact.ImpactClassification.ImpactType.*
import com.atlassian.performance.tools.report.api.impact.LatencyImpactClassifierTest.ActionTypes.SWITCH_NAV_VIEW
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
            ClassifiedLatencyImpact(BROWSE_BOARDS, ImpactClassification(NO_IMPACT, 1, 0), -0.0091, ofMillis(-3)),
            ClassifiedLatencyImpact(VIEW_BOARD, ImpactClassification(NO_IMPACT, 1, 0), -0.0156, ofMillis(-10)),
            ClassifiedLatencyImpact(VIEW_BACKLOG, ImpactClassification(NO_IMPACT, 1, 0), -0.0121, ofMillis(-7)),
            ClassifiedLatencyImpact(
                CREATE_ISSUE_SUBMIT,
                ImpactClassification(IMPROVEMENT, 1, 0),
                -0.0308,
                ofMillis(-14)
            ),
            ClassifiedLatencyImpact(EDIT_ISSUE_SUBMIT, ImpactClassification(IMPROVEMENT, 1, 0), -0.0206, ofMillis(-13)),
            ClassifiedLatencyImpact(
                ADD_COMMENT_SUBMIT,
                ImpactClassification(IMPROVEMENT, 1, 0),
                -0.0359,
                ofMillis(-23)
            ),
            ClassifiedLatencyImpact(CREATE_ISSUE, ImpactClassification(IMPROVEMENT, 1, 0), -0.1781, ofMillis(-711)),
            ClassifiedLatencyImpact(EDIT_ISSUE, ImpactClassification(IMPROVEMENT, 1, 0), -0.146, ofMillis(-263)),
            ClassifiedLatencyImpact(ADD_COMMENT, ImpactClassification(IMPROVEMENT, 1, 0), -0.1393, ofMillis(-165)),
            ClassifiedLatencyImpact(
                SEARCH_JQL_SIMPLE,
                ImpactClassification(IMPROVEMENT, 1, 0),
                -0.1152,
                ofMillis(-124)
            ),
            ClassifiedLatencyImpact(VIEW_ISSUE, ImpactClassification(NO_IMPACT, 1, 0), -0.0099, ofMillis(-5)),
            ClassifiedLatencyImpact(VIEW_DASHBOARD, ImpactClassification(REGRESSION, 1, 0), +0.0367, ofMillis(+11)),
            ClassifiedLatencyImpact(
                SEARCH_JQL_CHANGELOG,
                ImpactClassification(IMPROVEMENT, 1, 0),
                -0.0238,
                ofMillis(-48)
            ),
            ClassifiedLatencyImpact(PROJECT_SUMMARY, ImpactClassification(IMPROVEMENT, 1, 0), -0.0204, ofMillis(-5)),
            ClassifiedLatencyImpact(BROWSE_PROJECTS, ImpactClassification(NO_IMPACT, 1, 0), -0.0156, ofMillis(-6)),
            ClassifiedLatencyImpact(SWITCH_NAV_VIEW, ImpactClassification(IMPROVEMENT, 1, 0), -0.0633, ofMillis(-8))
        )
        val workspace = TestWorkspace(createTempDirectory(javaClass.simpleName))
        val table = LatencyImpactMarkdownTable(workspace)

        // when
        impacts.forEach { table.accept(it) }

        // then
        assertThat(workspace.directory.resolve("latency-impact-table.md")).hasContent(
            """
            | Action                | Classification | Confidence | Latency impact | Latency impact |
            |-----------------------|----------------|------------|----------------|----------------|
            | Browse Boards         | NO IMPACT      | 68.27 %    | -1 %           | -3 ms          |
            | View Board            | NO IMPACT      | 68.27 %    | -2 %           | -10 ms         |
            | View Backlog          | NO IMPACT      | 68.27 %    | -1 %           | -7 ms          |
            | Create Issue          | IMPROVEMENT    | 68.27 %    | -3 %           | -14 ms         |
            | Edit Issue            | IMPROVEMENT    | 68.27 %    | -2 %           | -13 ms         |
            | Add Comment           | IMPROVEMENT    | 68.27 %    | -4 %           | -23 ms         |
            | Full Create Issue     | IMPROVEMENT    | 68.27 %    | -18 %          | -711 ms        |
            | Full Edit Issue       | IMPROVEMENT    | 68.27 %    | -15 %          | -263 ms        |
            | Full Add Comment      | IMPROVEMENT    | 68.27 %    | -14 %          | -165 ms        |
            | Simple searches       | IMPROVEMENT    | 68.27 %    | -12 %          | -124 ms        |
            | View Issue            | NO IMPACT      | 68.27 %    | -1 %           | -5 ms          |
            | View Dashboard        | REGRESSION     | 68.27 %    | +4 %           | +11 ms         |
            | Changelog searches    | IMPROVEMENT    | 68.27 %    | -2 %           | -48 ms         |
            | Project Summary       | IMPROVEMENT    | 68.27 %    | -2 %           | -5 ms          |
            | Browse Projects       | NO IMPACT      | 68.27 %    | -2 %           | -6 ms          |
            | Switch issue nav view | IMPROVEMENT    | 68.27 %    | -6 %           | -8 ms          |
            """.trimIndent()
        )
    }

    @Test
    fun shouldShowConfidenceForRepeatedTests() {
        // given
        val impacts = listOf(
            ClassifiedLatencyImpact(BROWSE_BOARDS, ImpactClassification(IMPROVEMENT, 4, 0), -0.10, ofMillis(-30)),
            ClassifiedLatencyImpact(VIEW_BOARD, ImpactClassification(REGRESSION, 3, 1), +0.20, ofMillis(+60)),
            ClassifiedLatencyImpact(VIEW_BACKLOG, ImpactClassification(REGRESSION, 8, 5), +0.20, ofMillis(+60)),
            ClassifiedLatencyImpact(SEARCH_JQL_SIMPLE, ImpactClassification(IMPROVEMENT, 10, 2), -0.10, ofMillis(-30)),
            ClassifiedLatencyImpact(ADD_COMMENT, ImpactClassification(IMPROVEMENT, 26, 14), -0.10, ofMillis(-30)),
            ClassifiedLatencyImpact(BROWSE_PROJECTS, ImpactClassification(REGRESSION, 13, 10), +0.20, ofMillis(+60)),
            ClassifiedLatencyImpact(CREATE_ISSUE, ImpactClassification(INCONCLUSIVE, 0, 0), +0.01, ofMillis(+3)),
            ClassifiedLatencyImpact(VIEW_ISSUE, ImpactClassification(IMPROVEMENT, 3, 2), -0.10, ofMillis(-30)),
            ClassifiedLatencyImpact(VIEW_DASHBOARD, ImpactClassification(NO_IMPACT, 5, 0), +0.01, ofMillis(+3)),
            ClassifiedLatencyImpact(PROJECT_SUMMARY, ImpactClassification(NO_IMPACT, 8, 2), +0.01, ofMillis(+3))
        )
        val workspace = TestWorkspace(createTempDirectory(javaClass.simpleName))
        val table = LatencyImpactMarkdownTable(workspace)

        // when
        impacts.forEach { table.accept(it) }

        // then
        assertThat(workspace.directory.resolve("latency-impact-table.md")).hasContent(
            """
            | Action                | Classification | Confidence | Latency impact | Latency impact |
            |-----------------------|----------------|------------|----------------|----------------|
            | Browse Boards         | IMPROVEMENT    | 95.45 %    | -10 %          | -30 ms         |
            | View Board            | REGRESSION     | 68.27 %    | +20 %          | +60 ms         |
            | View Backlog          | REGRESSION     | 59.46 %    | +20 %          | +60 ms         |
            | Simple searches       | IMPROVEMENT    | 97.91 %    | -10 %          | -30 ms         |
            | Full Add Comment      | IMPROVEMENT    | 94.22 %    | -10 %          | -30 ms         |
            | Browse Projects       | REGRESSION     | 46.84 %    | +20 %          | +60 ms         |
            | Full Create Issue     | INCONCLUSIVE   | N/A        | N/A            | N/A            |
            | View Issue            | IMPROVEMENT    | 34.53 %    | -10 %          | -30 ms         |
            | View Dashboard        | NO IMPACT      | 97.47 %    | +1 %           | +3 ms          |
            | Project Summary       | NO IMPACT      | 94.22 %    | +1 %           | +3 ms          |
            """.trimIndent()
        )
    }
}
