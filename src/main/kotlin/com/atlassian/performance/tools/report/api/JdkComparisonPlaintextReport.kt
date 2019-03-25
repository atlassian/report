package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.ActionMetricStatistics
import org.apache.commons.lang3.StringUtils
import java.lang.StringBuilder
import java.time.Duration
import java.util.Formatter

class JdkComparisonPlaintextReport(
    val jdk8Statistics: ActionMetricStatistics,
    val jdk11Statistics: ActionMetricStatistics
) {
    fun generate(): String {
        val percentile = 95
        val jdk8Percentile = jdk8Statistics.percentile(percentile)
        val jdk11Percentile = jdk11Statistics.percentile(percentile)

        val report = StringBuilder()
        val formatter = Formatter(report)
        val lineFormat = "| %-25s | %-29d | %-28d | %-25d | %-9s |\n"

        formatter.format("\n")
        formatter.format("+---------------------------+-------------------------------+------------------------------+---------------------------+-----------+\n")
        formatter.format("| Action name               | JDK 11 - 95th percentile [ms] | JDK 8 - 95th percentile [ms] | Diff 95th percentile [ms] | Outcome   |\n")
        formatter.format("+---------------------------+-------------------------------+------------------------------+---------------------------|-----------+\n")

        jdk8Statistics
            .sampleSize
            .keys
            .sorted()
            .forEach { action ->
                val diff = compare(jdk8Percentile[action], jdk11Percentile[action])
                formatter.format(
                    lineFormat,
                    StringUtils.abbreviate(action, 25),
                    jdk11Percentile[action]?.toMillis(),
                    jdk8Percentile[action]?.toMillis(),
                    diff,
                    when {
                        diff < 0 -> "FASTER"
                        diff > 0 -> "SLOWER"
                        else -> "SAME"
                    }
                )
            }

        formatter.format("+---------------------------+-------------------------------+------------------------------+---------------------------+-----------+\n")
        return report.toString()
    }

    private fun compare(jdk8: Duration?, jdk11: Duration?): Long {
        val jdk8Millis = jdk8?.toMillis() ?: 0
        val jdk11Millis = jdk11?.toMillis() ?: 0

        return jdk11Millis - jdk8Millis
    }
}