package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.report.junit.JUnitReport
import org.apache.logging.log4j.LogManager
import java.nio.file.Path

data class Verdict(
    private val reports: List<JUnitReport>
) {
    private val logger = LogManager.getLogger(this::class.java)

    fun assertAccepted(
        testClassName: String,
        testResults: Path,
        expectedReportCount: Int? = null
    ) {
        reports.forEach { it.dump(testClassName, testResults.resolve("dynamic-$testClassName")) }

        if (expectedReportCount != null && expectedReportCount != reports.size) {
            throw Exception(
                "Performance results are rejected, because the test expects $expectedReportCount reports, "
                    + "but it yielded ${reports.size} reports"
            )
        }

        val fails = reports.filter { !it.successful }
        if (fails.isNotEmpty()) {
            throw Exception("Performance results are rejected, because:\n" + fails.joinToString(separator = "\n"))
        }
        logger.info("Performance results are accepted")
    }

    operator fun plus(other: Verdict) = Verdict(
        reports + other.reports
    )
}