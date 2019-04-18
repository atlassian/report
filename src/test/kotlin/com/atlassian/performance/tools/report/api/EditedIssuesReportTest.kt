package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.report.api.action.EditedIssuesReport
import com.atlassian.performance.tools.report.api.result.LocalRealResult
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.junit.Assert.assertThat
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class EditedIssuesReportTest {

    @Test
    fun shouldReport() {
        val output = Files.createTempFile("edited-issue-report", "csv")
        val results = listOf(
            "alpha",
            "beta"
        ).map { cohort ->
            Paths.get("JIRA-JPT760-JOB1-8").resolve(cohort)
        }.map { path ->
            LocalRealResult(path).loadEdible()
        }

        EditedIssuesReport().report(
            results = results,
            output = output
        )

        val rows = output.toFile().bufferedReader().use { it.readLines() }
        assertThat(rows[0], equalTo("issue key,JIRA-JPT760-JOB1-8/alpha,JIRA-JPT760-JOB1-8/beta"))
        assertThat(rows, hasItem("SP-688,1,0"))
        assertThat(rows, hasItem("AABA-615,1,3"))
        assertThat(rows, hasItem("AOBA-3,0,2"))
    }
}