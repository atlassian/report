package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.report.action.EditedIssuesReport
import com.atlassian.performance.tools.report.action.SearchJqlReport
import com.atlassian.performance.tools.report.distribution.DistributionComparison
import com.atlassian.performance.tools.workspace.TestWorkspace
import com.atlassian.performance.tools.workspace.git.GitRepo

/**
 * Judges each cohort result in absolute terms, ie. not relative to other cohorts.
 */
class IndependentCohortsJudge {

    private val repo = GitRepo.findFromCurrentDirectory()

    fun judge(
        results: List<EdibleResult>,
        report: TestWorkspace
    ): Verdict {
        DataReporter(
            output = report.directory.resolve("summary-per-cohort.csv").toFile(),
            labels = LinkedHashSet(results.flatMap { it.criticalActionLabels }).toList()
        ).report(results.map { it.actionStats })
        DistributionComparison(repo).compare(
            output = report.directory.resolve("distribution-comparison.html"),
            results = results
        )
        EditedIssuesReport().report(
            output = report.directory.resolve("edited-issues.csv"),
            results = results
        )
        return results
            .map { judge(it, report) }
            .reduce(Verdict::plus)
    }

    private fun judge(
        result: EdibleResult,
        report: TestWorkspace
    ): Verdict {
        val cohort = result.cohort
        val criteria = result.criteria
        val stats = result.actionStats
        val actionMetrics = result.allActionMetrics
        val cohortWorkspace = report.directory.resolve(cohort)
        TimelineChart(repo).generate(
            output = cohortWorkspace.resolve("time-series-chart.html"),
            actionMetrics = actionMetrics,
            systemMetrics = result.systemMetrics
        )
        SearchJqlReport(
            criteria = criteria,
            allMetrics = actionMetrics
        ).report(cohortWorkspace)
        val nodeDistribution = result.nodeDistribution
        return FailureJudge().judge(result.failure) +
            SampleSizeJudge().judge(stats, criteria.getSampleSizeCriteria()) +
            ErrorsJudge().judge(stats, criteria.getErrorCriteria()) +
            NodeBalanceJudge(criteria, cohort).judge(nodeDistribution) +
            VirtualUsersJudge(criteria).judge(nodeDistribution, cohort)
    }
}