package com.atlassian.performance.tools.report.api.action

import java.time.Duration

internal data class SearchJqlStats(
    val jql: String,
    val n: Long,
    val latency: Duration,
    val minTotalResults: Int,
    val maxTotalResults: Int
)