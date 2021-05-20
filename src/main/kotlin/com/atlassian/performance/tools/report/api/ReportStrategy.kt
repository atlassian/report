package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetricStatistics

object ReportStrategy {
    class CombinedReport(actionStats: ActionMetricStatistics): Report {
        private val csv = CSVReport(actionStats)
        private val plain = PlaintextReport(actionStats)

        override fun generate(): String {
            return plain.generate() + csv.generate()
        }
    }

    fun newReport(actionStats: ActionMetricStatistics): Report {
        // declared locally as value may change under test
        val outputFormat = System.getProperty("report.format") ?: "plain"

        return when (outputFormat) {
            "combined" -> CombinedReport(actionStats)
            "csv" -> CSVReport(actionStats)
            else -> PlaintextReport(actionStats)
        }
    }
}
