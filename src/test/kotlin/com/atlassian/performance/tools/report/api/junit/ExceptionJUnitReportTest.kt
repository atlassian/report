package com.atlassian.performance.tools.report.api.junit

import org.hamcrest.Matchers
import org.junit.Assert.assertThat
import org.junit.Test

class ExceptionJUnitReportTest {

    @Test
    fun shouldStartWithXmlDeclaration() {
        val report = ExceptionJUnitReport("TEST NAME", Exception("OOPS"))

        val xml = report.toXml("TEST CLASS NAME")

        assertThat(xml, Matchers.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
    }
}