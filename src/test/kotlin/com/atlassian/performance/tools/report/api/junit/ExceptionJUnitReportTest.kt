package com.atlassian.performance.tools.report.api.junit

import org.assertj.core.api.Assertions
import org.junit.Test
import java.io.PrintWriter

class ExceptionJUnitReportTest {

    @Test
    fun shouldGenerateXmlReport() {
        val report = ExceptionJUnitReport("TEST NAME", MockException("OOPS"))
        val expectedXml = this::class.java.getResourceAsStream("./jUnitReport.xml").bufferedReader().use { it.readText() }

        val xml = report.toXml("TEST CLASS NAME")

        Assertions.assertThat(xml).isEqualTo(expectedXml)
    }

    @Test
    fun shouldParsePercents() {
        val message = "OOPS%{"
        val report = ExceptionJUnitReport("TEST NAME", MockException(message))

        val xml = report.toXml("TEST CLASS NAME")

        Assertions.assertThat(xml).contains(message)
    }

    private class MockException(message: String) : Exception(message) {
        override fun printStackTrace(printWriter: PrintWriter) {
            printWriter.println("""java.lang.Exception: $message
	at com.atlassian.performance.tools.report.api.junit.ExceptionJUnitReportTest.shouldGenerateXmlReport(ExceptionJUnitReportTest.kt:10)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)""")
        }
    }
}