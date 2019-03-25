package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.io.api.ensureDirectory
import com.atlassian.performance.tools.jiraactions.api.ActionMetricStatistics
import com.atlassian.performance.tools.report.api.action.EditedIssuesReport
import com.atlassian.performance.tools.report.api.action.SearchJqlReport
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.chart.MeanLatencyChart
import com.atlassian.performance.tools.report.chart.TimelineChart
import com.atlassian.performance.tools.report.distribution.DistributionComparison
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import com.atlassian.performance.tools.workspace.api.git.GitRepo
import org.apache.logging.log4j.LogManager

class FullReport {
    private val repo = GitRepo.findFromCurrentDirectory()
    private val logger = LogManager.getLogger(this::class.java)

    fun dump(
        results: List<EdibleResult>,
        workspace: TestWorkspace,
        labels: List<String> = results.flatMap { it.actionLabels }.toSet().sorted()
    ) {
        val stats = results.map { it.actionStats }
        DataReporter(
            output = workspace.directory.resolve("summary-per-cohort.csv").toFile(),
            labels = labels
        ).report(stats)

        MeanLatencyChart().plot(
            stats = stats,
            labels = labels,
            output = workspace.directory.resolve("mean-latency-chart.html").toFile()
        )

        DistributionComparison(repo).compare(
            output = workspace.directory.resolve("distribution-comparison.html"),
            results = results
        )
        EditedIssuesReport().report(
            output = workspace.directory.resolve("edited-issues.csv"),
            results = results
        )

        val jdkBenchmarkTest = results.any { result -> result.cohort.contains("jdk") }
        if (jdkBenchmarkTest) {
            generateJdkComparisonReport(results)
        }

        return results.forEach { result ->
            val actionMetrics = result.actionMetrics
            val cohortWorkspace = workspace.directory.resolve(result.cohort)
            TimelineChart(repo).generate(
                output = cohortWorkspace.resolve("time-series-chart.html"),
                actionMetrics = actionMetrics,
                systemMetrics = result.systemMetrics
            )

            val report = PlaintextReport(
                ActionMetricStatistics(actionMetrics)
            ).generate()

            logger.info("Plain text report:\n$report")
            SearchJqlReport(
                allMetrics = actionMetrics
            ).report(cohortWorkspace)

            WaterfallHighlightReport().report(
                metrics = actionMetrics,
                workspace = TestWorkspace(cohortWorkspace.resolve("WaterfallHighlight").ensureDirectory())
            )
        }
    }

    private fun generateJdkComparisonReport(results: List<EdibleResult>) {
        val jdk8Result = results.first { result -> result.cohort == "jdk8" }
        val jdk11Result = results.first { result -> result.cohort == "jdk11" }

        val report = JdkComparisonPlaintextReport(
            ActionMetricStatistics(jdk8Result.actionMetrics),
            ActionMetricStatistics(jdk11Result.actionMetrics)
        ).generate()

        logger.info("JDK 11 vs JDK 8 report:\n$report")
    }
}