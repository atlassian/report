package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.report.api.result.LocalRealResult
import org.assertj.core.api.Assertions
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class MeanLatencyChartTest {

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

    /**
     * If you want to update the expected HTML, just copy the contents of the file located by the printed path.
     * Don't use IDEA to paste, because it reformats the content. Use a simple text editor or bash.
     */
    @Test
    fun shouldOutputHtml() {
        //given
        val output = Paths.get("build/actual-latency-chart.html")
        val labels = actionTypes.map { it.label }
        val results = listOf(
                LocalRealResult(Paths.get("JIRA-JPT760-JOB1-8/alpha")).loadEdible(),
                LocalRealResult(Paths.get("JIRA-JPT760-JOB1-8/beta")).loadEdible()
        )
        val stats = results.map { it.stats }

        // when
        MeanLatencyChart().plot(
                stats = stats,
                labels = labels,
                output = output.toFile()
        )

        // then
        println("Mean latency chart is available at $output")
        val actualOutput = output.toFile()
        val expectedOutput = File(javaClass.getResource("expected-latency-chart.html").toURI())
        Assertions.assertThat(actualOutput).hasSameContentAs(expectedOutput)
    }
}