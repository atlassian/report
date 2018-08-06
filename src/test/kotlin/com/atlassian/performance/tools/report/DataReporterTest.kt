package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.jiraactions.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Duration

class DataReporterTest {

    @Rule
    @JvmField
    val workDir = TemporaryFolder()

    private val labels: List<String> = listOf(
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
    ).map { it.label }

    private val stats = InteractionStats(
        cohort = "7.8.0 : Standalone : 1M issues",
        sampleSizes = mapOf(
            "View Issue" to 9L,
            "Add Comment" to 1L
        ),
        centers = mapOf(
            "Add Comment" to Duration.parse("PT1.000000000S"),
            "View Issue" to Duration.parse("PT2.000000000S")
        ),
        dispersions = mapOf(
            "Add Comment" to Duration.parse("PT0.000000000S"),
            "View Issue" to Duration.parse("PT0.000000000S")
        ),
        errors = mapOf(
            "Add Comment" to 0,
            "View Issue" to 0
        )
    )

    @Test
    fun shouldAggregateAverage() {
        val report = workDir.newFile()

        DataReporter(report, labels).report(listOf(stats))

        val expectedAverage = "7.8.0 : Standalone : 1M issues,response time average,,2000,,,1000,,,,,,1900"
        val actualAverage = report.reader().readLines()[1]
        assertEquals(expectedAverage, actualAverage)
    }

    @Test
    fun shouldPrintHeader() {
        val report = workDir.newFile()

        DataReporter(report, labels).report(listOf(stats))

        val expectedHeader = "cohort,metric,View Board,View Issue,View Dashboard,Search with JQL,Add Comment," +
            "Create Issue,Edit Issue,Project Summary,Browse Projects,Browse Boards,AGGREGATE"
        val actualHeader = report.reader().readLines()[0]
        assertEquals(expectedHeader, actualHeader)
    }

    @Test
    fun shouldCreateReport() {
        val stats = listOf(
            "1M issues",
            "2M issues",
            "2800 custom fields",
            "1.3M attachments",
            "2900 boards",
            "200k users and 45k groups",
            "900 workflows",
            "340 security levels and 400 permissions",
            "5.8M comments",
            "3000 projects"
        ).flatMap { datasetLabel ->
            listOf("7.8.0", "7.9.0").flatMap { version ->
                listOf("Standalone", "Data Center").map { topology ->
                    stats(version, topology, datasetLabel)
                }
            }
        }
        val expectedReport = javaClass.getResource("./expected-summary-per-cohort.csv").readText()
        val report = workDir.newFile()

        DataReporter(report, labels).report(stats)

        val actualReport = report.reader().readText()
        assertEquals(expectedReport, actualReport)
    }

    @Test
    fun shouldReportPartialCohorts() {
        val stats = listOf(
            stats("1.0.0", "star", "tiny"),
            stats("2.0.0", "ring", "medium").copy(
                sampleSizes = null,
                centers = null,
                dispersions = null,
                errors = null
            ),
            stats("3.0.0", "bus", "large")
        )
        val report = workDir.newFile()

        DataReporter(report, labels).report(stats)

        val expectedReport = javaClass.getResource("./expected-partial-report.csv").readText()
        val actualReport = report.reader().readText()
        assertEquals(expectedReport, actualReport)
    }

    private fun stats(
        version: String,
        topology: String,
        datasetLabel: String
    ): InteractionStats = InteractionStats(
        cohort = "$version : $topology : $datasetLabel",
        sampleSizes = mapOf(
            "View Board" to 202L,
            "View Issue" to 1302L,
            "View Dashboard" to 234L,
            "Search with JQL" to 486L,
            "Add Comment" to 46L,
            "Create Issue" to 109L,
            "Edit Issue" to 118L,
            "Project Summary" to 107L,
            "Browse Projects" to 119L,
            "Browse Boards" to 45L
        ),
        centers = mapOf(
            "View Board" to Duration.parse("PT2.868775S"),
            "View Issue" to Duration.parse("PT0.460214119S"),
            "View Dashboard" to Duration.parse("PT0.648551724S"),
            "Search with JQL" to Duration.parse("PT9.285082987S"),
            "Add Comment" to Duration.parse("PT1.808652173S"),
            "Create Issue" to Duration.parse("PT2.605166666S"),
            "Edit Issue" to Duration.parse("PT2.19734188S"),
            "Project Summary" to Duration.parse("PT1.261849056S"),
            "Browse Projects" to Duration.parse("PT0.750847457S"),
            "Browse Boards" to Duration.parse("PT1.326955555S")
        ),
        dispersions = mapOf(
            "View Board" to Duration.parse("PT1.52123081S"),
            "View Issue" to Duration.parse("PT0.213154215S"),
            "View Dashboard" to Duration.parse("PT0.12064264S"),
            "Search with JQL" to Duration.parse("PT6.682741637S"),
            "Add Comment" to Duration.parse("PT0.243675304S"),
            "Create Issue" to Duration.parse("PT0.186234326S"),
            "Edit Issue" to Duration.parse("PT0.255830094S"),
            "Project Summary" to Duration.parse("PT0.191439598S"),
            "Browse Projects" to Duration.parse("PT0.094599436S"),
            "Browse Boards" to Duration.parse("PT1.805332781S")
        ),
        errors = mapOf(
            "View Board" to 0,
            "View Issue" to 0,
            "View Dashboard" to 0,
            "Search with JQL" to 0,
            "Add Comment" to 0,
            "Create Issue" to 0,
            "Edit Issue" to 0,
            "Project Summary" to 0,
            "Browse Projects" to 0,
            "Browse Boards" to 0
        )
    )
}