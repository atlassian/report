package com.atlassian.performance.tools.report.jstat

import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JstatConverterTest {

    @Rule
    @JvmField
    val folder = TemporaryFolder()

    private val jstatConverter = JstatConverter()

    @Test
    fun shouldConvertVmstatLog() {
        val logFile = folder.newFile("jstat.log")
        this::class.java.getResourceAsStream("./jstat.log").copyTo(logFile.outputStream())
        val expectedCsv = this::class.java.getResourceAsStream("./jstat.csv").bufferedReader().use { it.readText() }

        val actualCsv = jstatConverter.convertToCsv(logFile.toPath()).toFile().bufferedReader().use { it.readText() }

        assertThat(actualCsv, equalTo(expectedCsv))
    }

    @Test
    fun shouldCreateFreshCsvOnEachRun() {
        val logFile = folder.newFile("jstat.log")
        this::class.java.getResourceAsStream("./jstat.log").copyTo(logFile.outputStream())

        val firstCsvResult = jstatConverter.convertToCsv(logFile.toPath()).toFile().bufferedReader().use { it.readText() }
        val secondCsvResult = jstatConverter.convertToCsv(logFile.toPath()).toFile().bufferedReader().use { it.readText() }

        Assertions.assertThat(firstCsvResult).isEqualTo(secondCsvResult)
    }

}