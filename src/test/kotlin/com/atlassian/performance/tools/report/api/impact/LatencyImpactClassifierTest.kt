package com.atlassian.performance.tools.report.api.impact

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.jirasoftwareactions.api.actions.WorkOnBacklog.Companion.VIEW_BACKLOG
import com.atlassian.performance.tools.report.api.impact.ImpactClassification.ImpactType.*
import com.atlassian.performance.tools.report.api.impact.LatencyImpactClassifierTest.ActionTypes.SWITCH_NAV_VIEW
import com.atlassian.performance.tools.report.api.judge.LatencyImpact
import com.atlassian.performance.tools.report.api.judge.LatencyImpact.Builder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration.ofMillis

class LatencyImpactClassifierTest {

    object ActionTypes {
        val SWITCH_NAV_VIEW = ActionType("Switch issue nav view") { }
    }

    /**
     * Equivalent of [an internal report](https://hello.atlassian.net/l/cp/jCtvREHf).
     */
    @Test
    fun shouldSummarizeReportFromApril2023() {
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
            Builder(SWITCH_NAV_VIEW, -0.0633, ofMillis(-8)).relevant().build()
        )
        val summary = LatencyImpactClassifier.Builder().build()

        // when
        impacts.forEach { summary.accept(it) }
        val actualSummary = summary.classify()

        // then
        val expectedSummary = listOf(
            ClassifiedLatencyImpact(BROWSE_BOARDS, ImpactClassification(NO_IMPACT, 1, 0), -0.0091, ofMillis(-3)),
            ClassifiedLatencyImpact(VIEW_BOARD, ImpactClassification(NO_IMPACT, 1, 0), -0.0156, ofMillis(-10)),
            ClassifiedLatencyImpact(VIEW_BACKLOG, ImpactClassification(NO_IMPACT, 1, 0), -0.0121, ofMillis(-7)),
            ClassifiedLatencyImpact(CREATE_ISSUE_SUBMIT, ImpactClassification(IMPROVEMENT, 1, 0), -0.0308, ofMillis(-14)),
            ClassifiedLatencyImpact(EDIT_ISSUE_SUBMIT, ImpactClassification(IMPROVEMENT, 1, 0), -0.0206, ofMillis(-13)),
            ClassifiedLatencyImpact(ADD_COMMENT_SUBMIT, ImpactClassification(IMPROVEMENT, 1, 0), -0.0359, ofMillis(-23)),
            ClassifiedLatencyImpact(CREATE_ISSUE, ImpactClassification(IMPROVEMENT, 1, 0), -0.1781, ofMillis(-711)),
            ClassifiedLatencyImpact(EDIT_ISSUE, ImpactClassification(IMPROVEMENT, 1, 0), -0.146, ofMillis(-263)),
            ClassifiedLatencyImpact(ADD_COMMENT, ImpactClassification(IMPROVEMENT, 1, 0), -0.1393, ofMillis(-165)),
            ClassifiedLatencyImpact(SEARCH_JQL_SIMPLE, ImpactClassification(IMPROVEMENT, 1, 0), -0.1152, ofMillis(-124)),
            ClassifiedLatencyImpact(VIEW_ISSUE, ImpactClassification(NO_IMPACT, 1, 0), -0.0099, ofMillis(-5)),
            ClassifiedLatencyImpact(VIEW_DASHBOARD, ImpactClassification(REGRESSION, 1, 0), +0.0367, ofMillis(+11)),
            ClassifiedLatencyImpact(SEARCH_JQL_CHANGELOG, ImpactClassification(IMPROVEMENT, 1, 0), -0.0238, ofMillis(-48)),
            ClassifiedLatencyImpact(PROJECT_SUMMARY, ImpactClassification(IMPROVEMENT, 1, 0), -0.0204, ofMillis(-5)),
            ClassifiedLatencyImpact(BROWSE_PROJECTS, ImpactClassification(NO_IMPACT, 1, 0), -0.0156, ofMillis(-6)),
            ClassifiedLatencyImpact(SWITCH_NAV_VIEW, ImpactClassification(IMPROVEMENT, 1, 0), -0.0633, ofMillis(-8))
        )
        assertThat(actualSummary).containsExactlyElementsOf(expectedSummary)
        assertThat(actualSummary).allSatisfy {
            assertThat(it.classification.confidence).isEqualTo(0.682689492137086)
        }
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
        val summary = LatencyImpactClassifier.Builder().build()

        // when
        impacts.forEach { summary.accept(it) }
        val actualSummary = summary.classify() // TODO avoid "summary" name conflict with LatencyImpactSummary

        // then
        val expectedSummary = listOf(
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
        assertThat(actualSummary).containsExactlyElementsOf(expectedSummary)
        val expectedConfidences = listOf(
            0.9544997361036417,
            0.682689492137086,
            0.5946194435410576,
            0.9790786646622059,
            0.9422204288764027,
            0.4683854231183877,
            Double.NaN,
            0.345279153981423,
            0.9746526813225317,
            0.9422204288764027
        )
        assertThat(actualSummary.map { it.classification.confidence }).containsExactlyElementsOf(expectedConfidences)
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
