package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.report.action.EditedIssuesReport
import com.atlassian.performance.tools.report.action.SearchJqlReport
import com.atlassian.performance.tools.report.distribution.DistributionComparison
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import com.atlassian.performance.tools.workspace.api.git.GitRepo

class FullReport {
    private val repo = GitRepo.findFromCurrentDirectory()

    fun dump(
        results: List<EdibleResult>,
        workspace: TestWorkspace,
        labels: List<String> = results.flatMap { it.actionLabels }.toSet().sorted()
    ) {
        DataReporter(
            output = workspace.directory.resolve("summary-per-cohort.csv").toFile(),
            labels = labels
        ).report(results.map { it.actionStats })

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
            SearchJqlReport(
                allMetrics = actionMetrics
            ).report(cohortWorkspace)
        }
    }
}