package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.report.api.result.LocalRealResult
import com.atlassian.performance.tools.workspace.api.git.HardcodedGitRepo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class TimelineChartTest {

    @Test
    fun shouldGenerateOutput() {
        val result = LocalRealResult(Paths.get("QUICK-54/c5.18xlarge, 2 nodes, run 1.zip")).loadEdible()
        val actualOutput = Files.createTempFile("apt-report-test-timeline", ".html")
        val chart = TimelineChart(HardcodedGitRepo(head = "abcd"))

        chart.generate(
            actualOutput,
            result.actionMetrics,
            result.systemMetrics
        )

        val expectedOutput = File(javaClass.getResource("expected-timeline-chart.html").toURI()).toPath()
        assertThat(actualOutput).hasSameContentAs(expectedOutput)
    }
}
