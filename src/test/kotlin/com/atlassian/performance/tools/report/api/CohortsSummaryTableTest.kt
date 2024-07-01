package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.report.api.result.LocalScalingResult
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Paths

class CohortsSummaryTableTest {
    @Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    @Test
    fun shouldCreateTableReport() {
        val actualOutput = Paths.get("build/actual-aui-table.html").toFile()

        val results = LocalScalingResult(Paths.get("DSR/7.13.1v7.6.10.zip")).loadAll(tempFolder)
        CohortsSummaryTable(
                output = actualOutput,
                labels = results[0].actionLabels.toList()
        ).report(results.map { it.stats })

        val expectedOutput = File(javaClass.getResource("expected-aui-table.html").toURI())
        Assertions.assertThat(actualOutput).hasSameContentAs(expectedOutput)
    }
}
