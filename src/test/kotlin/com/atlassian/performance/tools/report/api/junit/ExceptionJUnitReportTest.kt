package com.atlassian.performance.tools.report.api.junit

import org.assertj.core.api.Assertions
import org.junit.Test

class ExceptionJUnitReportTest {

    @Test
    fun shouldStartWithXmlDeclaration() {
        val report = ExceptionJUnitReport("TEST NAME", Exception("OOPS"))

        val xml = report.toXml("TEST CLASS NAME")

        Assertions.assertThat(xml).startsWith("""<?xml version="1.0" encoding="UTF-8"?>""")
    }

    @Test
    fun shouldParsePercents() {
        val message = "OOPS%{"
        val report = ExceptionJUnitReport("TEST NAME", Exception(message))

        val xml = report.toXml("TEST CLASS NAME")

        Assertions.assertThat(xml).contains(message)
    }
}