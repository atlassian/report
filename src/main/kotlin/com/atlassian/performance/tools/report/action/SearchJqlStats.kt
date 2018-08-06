package com.atlassian.performance.tools.report.action

import java.time.Duration

data class SearchJqlStats(
    val jql: String,
    val n: Long,
    val latency: Duration,
    val minTotalResults: Int,
    val maxTotalResults: Int
)