package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.junit.FailedAssertionJUnitReport
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import java.util.*

class Verdict internal constructor(
    /**
     * @since 3.10.0
     */
    val reports: List<JUnitReport>,
    val failedActions: List<ActionType<*>>
) {
    private val logger = LogManager.getLogger(this::class.java)

    @Deprecated(message = "Use Builder instead", replaceWith = ReplaceWith("Verdict.Builder(reports).build()"))
    constructor(
        reports: List<JUnitReport>
    ) : this(
        reports = reports,
        failedActions = emptyList()
    )

    /**
     * Since 3.12.0
     */
    class Builder(reports: List<JUnitReport>) {
        private var failedActions: MutableList<ActionType<*>> = ArrayList()
        private var reports: MutableList<JUnitReport> = ArrayList(reports)

        fun failedActions(failedActions: List<ActionType<*>>): Builder = apply {
            this.failedActions = ArrayList(failedActions)
        }

        fun addReports(reports: List<JUnitReport>): Builder = apply {
            this.reports.addAll(reports)
        }

        fun build(): Verdict {
            return Verdict(
                reports = Collections.unmodifiableList(reports),
                failedActions = Collections.unmodifiableList(failedActions)
            )
        }
    }


    /**
     * @since 3.9.0
     */
    val positive: Boolean = reports.all { it.successful }

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

    operator fun plus(other: Verdict) = Builder(reports = reports + other.reports).build()

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
