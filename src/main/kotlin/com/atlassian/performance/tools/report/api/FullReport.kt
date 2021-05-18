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
    private val outputFormat = System.getProperty("FullReport.format") ?: "plain"

    /**
     * Produce all known useful reports for both comparisons and individual results.
     *
     * ### Comparisons
     * Compare different [results]. Each result is treated equally.
     *
     * #### Cohort summary
     * Tabulate aggregated stats of all [results] in CSV and HTML formats.
     *
     * #### Distribution comparison
     * Display the entire distribution of action metric durations.
     * Break down per action type.
     * Since 3.7.0, include the distribution of an entire result.
     *
     * ### Individual
     * Report on each of the [results] in an isolated subdirectory in the [workspace].
     *
     * #### Timeline chart
     * Plot action metrics, system metrics and VU stats over time.
     *
     * #### Waterfall highlight
     * Chart waterfalls of characteristic action metrics, if captured.
     */
    fun dump(
        results: List<EdibleResult>,
        workspace: TestWorkspace,
        labels: List<String> = results.flatMap { it.actionLabels }.toSet().sorted()
    ) {
        val stats = results.map { it.stats }
        CohortStatsSummary(
            output = workspace.directory.resolve("summary-per-cohort.csv").toFile(),
            labels = labels
        ).report(stats)

        CohortsSummaryTable(
            output = workspace.directory.resolve("summary-per-cohort.html").toFile(),
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

        return results.forEach { result ->
            val actionMetrics = result.actionMetrics
            val cohortWorkspace = workspace.directory.resolve(result.cohort)
            TimelineChart(repo).generate(
                output = cohortWorkspace.resolve("time-series-chart.html"),
                actionMetrics = actionMetrics,
                systemMetrics = result.systemMetrics
            )

            val actionStats = ActionMetricStatistics(result.actionMetrics)
            val report = if (outputFormat == "csv") {
                CSVReport(actionStats).generate()
            } else {
                PlaintextReport(actionStats).generate()
            }

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
}
