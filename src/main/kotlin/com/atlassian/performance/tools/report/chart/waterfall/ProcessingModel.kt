package com.atlassian.performance.tools.report.chart.waterfall

import java.net.URI
import java.time.Duration

internal data class ProcessingModel(
    val address: URI,
    val phases: Map<Phase, Duration>,
    val initiatorType: String,
    val transferSize: Long,
    val decodedBodySize: Long,
    val totalDuration: Duration
)