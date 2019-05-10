package com.atlassian.performance.tools.report.chart.waterfall

import java.time.Duration

internal data class ProcessingModel(
    val address: String,
    val phases: Map<Phase, Duration>,
    val initiatorType: String,
    val transferSize: Long,
    val decodedBodySize: Long,
    val totalDuration: Duration
)
