package com.atlassian.performance.tools.report.vmstat

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VmstatConverterTest {

    @Rule
    @JvmField
    val folder = TemporaryFolder()

    private val vmstatConverter = VmstatConverter()

    @Test
    fun shouldConvertVmstatLog() {

        val logFile = folder.newFile("vmstat.log")
        this::class.java.getResourceAsStream("./vmstat.log").copyTo(logFile.outputStream())
        val expectedCsv = this::class.java.getResourceAsStream("./vmstat.csv").bufferedReader().readText()

        val actualCsv = vmstatConverter.convertToCsv(logFile.toPath()).toFile().bufferedReader().readText()

        assertThat(actualCsv, equalTo(expectedCsv))
    }
}