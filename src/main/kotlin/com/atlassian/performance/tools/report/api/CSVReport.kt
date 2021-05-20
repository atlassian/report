package com.atlassian.performance.tools.report.api


import com.atlassian.performance.tools.jiraactions.api.ActionMetricStatistics

class CSVReport(
    private val actionMetricStatistics: ActionMetricStatistics
) : Report {
    private val percentileLevels = listOf(50, 95, 99, 100)
    private val percentiles = percentileLevels.map{ it to actionMetricStatistics.percentile(it) }.toMap()

    override fun generate(): String {
        val report = StringBuilder()
        report.append("actionName,sampleSize,errors,total,p50,p95,p99,max\n")
        actionMetricStatistics
            .sampleSize
            .keys // i.e. actionNames
            .sorted()
            .map { actionReportLine(it) }
            .forEach { report.append(it) }
        return report.toString()
    }

    private fun actionReportLine(actionName: String): String {
        val samples = actionMetricStatistics.sampleSize[actionName] ?: 0
        val errors = actionMetricStatistics.errors[actionName] ?: 0
        val total = samples + errors
        val p = actionTimingPercentiles(actionName)

        return String.format("%s,%d,%d,%d,%s,%s,%s,%s\n",
            actionName, samples, errors, total, p[50], p[95], p[99], p[100])
    }

    /**
     * extract a map of percentile levels to values just for one action.
     */
    private fun actionTimingPercentiles(actionName: String) =
        percentiles.map { byNumber ->
            (byNumber.key to (byNumber.value[actionName]?.toMillis() ?: ""))
        }.toMap()
}
