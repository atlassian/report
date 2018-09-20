package com.atlassian.performance.tools.report.api.junit

import java.io.PrintWriter
import java.io.StringWriter

internal class ExceptionJUnitReport(
    override val testName: String,
    private val exception: Exception
) : JUnitReport {

    override val successful = false

    override fun toXml(
        testClassName: String
    ): String {
        val stackTrace = StringWriter()
        exception.printStackTrace(PrintWriter(stackTrace))
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="$testClassName" tests="1" skipped="0" failures="1" errors="0">
            <testcase name="$testName" classname="$testClassName">
                <failure message="${exception.message}" type="${exception.javaClass.name}">%s</failure>
            </testcase>
            </testsuite>
        """.trimIndent().format(stackTrace)
    }

    override fun toString(): String = "${exception.javaClass.name}: ${exception.message ?: ""}"
}