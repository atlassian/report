package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import org.apache.logging.log4j.LogManager
import java.nio.file.Path

class Verdict internal constructor(
    val actionReports: Collection<ActionReport>
) {

    constructor(
        reports: List<JUnitReport>
    ) : this(
        actionReports = reports.map {
            ActionReport(
                report = it,
                action = null,
                critical = !it.successful
            )
        }
    )

    @Deprecated("Use actionReports instead.")
    val failedActions: List<ActionType<*>>  = actionReports
        .filter { !it.successful }
        .mapNotNull { it.action }

    private val logger = LogManager.getLogger(this::class.java)
    internal val positive: Boolean = actionReports.all { it.successful }

    fun dump(
        testClassName: String,
        testResults: Path,
        expectedReportCount: Int? = null
    ) {
        val allReports = actionReports.map { it.report } + checkIfNoReportIsMissing(expectedReportCount)
        allReports.forEach { it.dump(testClassName, testResults.resolve("dynamic-$testClassName")) }
    }

    fun assertAccepted(
        testClassName: String,
        testResults: Path,
        expectedReportCount: Int? = null
    ) {
        dump(testClassName, testResults, expectedReportCount)
        val allReports = actionReports.map { it.report } + checkIfNoReportIsMissing(expectedReportCount)
        val fails = allReports.filter { !it.successful }
        if (fails.isNotEmpty()) {
            throw Exception("Performance results are rejected, because:\n" + fails.joinToString(separator = "\n"))
        }
        logger.info("Performance results are accepted")
    }

    operator fun plus(other: Verdict) = Verdict(
        actionReports + other.actionReports
    )

    private fun checkIfNoReportIsMissing(
        expectedReportCount: Int?
    ): JUnitReport {
        val testName = "Report meta-check"
        return when {
            expectedReportCount == null -> SuccessfulJUnitReport(testName = testName)
            expectedReportCount != actionReports.size -> FailedAssertionJUnitReport(
                assertion = "$expectedReportCount reports were expected, but ${actionReports.size} reports were yielded",
                testName = testName
            )
            else -> SuccessfulJUnitReport(testName = testName)
        }
    }
}
