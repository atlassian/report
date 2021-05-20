package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetricStatistics
import com.atlassian.performance.tools.testutil.SystemPropertyRule
import com.atlassian.performance.tools.testutil.WithSystemProperty
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReportStrategyTest {

    @get:Rule val rule = SystemPropertyRule()

    @Test
    @WithSystemProperty("report.format", "garbage")
    fun defaultIsPlain() {
        val report = ReportStrategy.newReport(ActionMetricStatistics(emptyList()))
        assertTrue(report is PlaintextReport)
    }

    @Test
    @WithSystemProperty("report.format", "plain")
    fun explicitPlain() {
        val report = ReportStrategy.newReport(ActionMetricStatistics(emptyList()))
        assertTrue(report is PlaintextReport)
    }

    @Test
    @WithSystemProperty("report.format", "csv")
    fun csv() {
        val report = ReportStrategy.newReport(ActionMetricStatistics(emptyList()))
        assertTrue(report is CSVReport)
    }

    @Test
    @WithSystemProperty("report.format", "combined")
    fun combined() {
        val report = ReportStrategy.newReport(ActionMetricStatistics(emptyList()))
        assertTrue(report is ReportStrategy.CombinedReport)
    }
}
