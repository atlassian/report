package com.atlassian.performance.tools.report.api.action

import com.atlassian.performance.tools.report.api.result.LocalRealResult
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files.createTempDirectory
import java.nio.file.Paths

class JqlReportTest {
    @Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    @Test
    fun shouldReportJqlsFromVariousActionTypes() {
        val results = LocalRealResult(Paths.get("JPT-JPTINT192-CHECK-2")).loadEdible(tempFolder)
        val workspace = TestWorkspace(createTempDirectory(javaClass.simpleName))

        JqlReport.Builder()
            .build()
            .report(results.actionMetrics, workspace.directory)

        assertThat(
            workspace.directory.resolve("search-jql-stats.csv").toFile()
        ).hasContent(
            """
            jql,n,latency,minTotalResults,maxTotalResults
            resolved is not empty order by description,5,PT0.867S,731294,731294
            assignee = admin order by project,3,PT1.073333333S,1000029,1000124
            reporter was admin order by description,11,PT1.884727272S,1000044,1000269
            project = SP and assignee = admin order by reporter,6,PT0.8595S,678,678
            """.trimIndent()
        )
    }
}
