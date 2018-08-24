package com.atlassian.performance.tools.report.api.junit

internal class SuccessfulJUnitReport(
    override val testName: String
) : JUnitReport {

    override val successful = true

    override fun toXml(
        testClassName : String
    ): String =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <testsuite name="$testClassName" tests="1" skipped="0" failures="0" errors="0">
        <testcase name="$testName" classname="$testClassName"/>
        </testsuite>
        """.trimIndent()
}