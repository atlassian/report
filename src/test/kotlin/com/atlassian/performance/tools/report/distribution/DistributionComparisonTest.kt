package com.atlassian.performance.tools.report.distribution

import com.atlassian.performance.tools.report.api.result.LocalRealResult
import com.atlassian.performance.tools.workspace.api.git.HardcodedGitRepo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Paths

class DistributionComparisonTest {
    @Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    /**
     * If you want to update the expected HTML, just copy the contents of the file located by the printed path.
     * Don't use IDEA to paste, because it reformats the content. Use a simple text editor or bash.
     */
    @Test
    fun shouldOutputHtml() {
        val output = Paths.get("build/actual-distribution-comparison.html")
        val repo = HardcodedGitRepo(head = "1234")

        DistributionComparison(repo).compare(
            output = output,
            results = listOf(
                LocalRealResult(Paths.get("JIRA-JPT760-JOB1-8/alpha")).loadEdible(tempFolder),
                LocalRealResult(Paths.get("JIRA-JPT760-JOB1-8/beta")).loadEdible(tempFolder)
            )
        )

        println("Test distribution comparison available at $output")
        val actualOutput = output.toFile()
        val expectedOutput = File(javaClass.getResource("expected-distribution-comparison.html").toURI())
        assertThat(actualOutput).hasSameContentAs(expectedOutput)
    }
}
