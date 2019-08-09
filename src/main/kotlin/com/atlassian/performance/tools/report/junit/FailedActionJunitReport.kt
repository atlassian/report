package com.atlassian.performance.tools.report.junit

import com.atlassian.performance.tools.report.api.junit.JUnitReport

internal class FailedActionJunitReport(
    override val testName: String,
    private val assertion: String
) : JUnitReport {

    override val successful = false

    override fun toXml(
        testClassName: String
    ): String =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <testsuite name="$testClassName" tests="1" skipped="0" failures="1" errors="0">
        <testcase name="$testName" classname="$testClassName">
            <failure message="$assertion" type="ActionFailure"/>
        </testcase>
        </testsuite>
        """.trimIndent()

    override fun toString(): String = assertion

}
