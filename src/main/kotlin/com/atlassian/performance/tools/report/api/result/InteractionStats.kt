package com.atlassian.performance.tools.report.api.result

import java.time.Duration

data class InteractionStats(
    internal val cohort: String,
    internal val sampleSizes: Map<String, Long>?,
    internal val centers: Map<String, Duration>?,
    internal val dispersions: Map<String, Duration>?,
    internal val errors: Map<String, Int>?
)