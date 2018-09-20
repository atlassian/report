package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import org.apache.logging.log4j.LogManager
import java.nio.file.Path

data class Verdict(
    private val reports: List<JUnitReport>
) {
    private val logger = LogManager.getLogger(this::class.java)
    internal val positive: Boolean = reports.all { it.successful }

    fun assertAccepted(
        testClassName: String,
        testResults: Path,
        expectedReportCount: Int? = null
    ) {
        val allReports = reports + checkIfNoReportIsMissing(expectedReportCount)
        allReports.forEach { it.dump(testClassName, testResults.resolve("dynamic-$testClassName")) }
        val fails = allReports.filter { !it.successful }
        if (fails.isNotEmpty()) {
            throw Exception("Performance results are rejected, because:\n" + fails.joinToString(separator = "\n"))
        }
        logger.info("Performance results are accepted")
    }

    operator fun plus(other: Verdict) = Verdict(
        reports + other.reports
    )

    private fun checkIfNoReportIsMissing(
        expectedReportCount: Int?
    ): JUnitReport {
        val testName = "Report meta-check"
        return when {
            expectedReportCount == null -> SuccessfulJUnitReport(testName = testName)
            expectedReportCount != reports.size -> FailedAssertionJUnitReport(
                assertion = "$expectedReportCount reports were expected, but ${reports.size} reports were yielded",
                testName = testName
            )
            else -> SuccessfulJUnitReport(testName = testName)
        }
    }
}
