package com.atlassian.performance.tools.report.api.result

import java.time.Duration

/**
 * @deprecated The generated `copy` and `componentN` methods should not be used. It will become a non-data class.
 */
data class InteractionStats(
    internal val cohort: String,
    internal val sampleSizes: Map<String, Long>?,
    internal val centers: Map<String, Duration>?,
    internal val dispersions: Map<String, Duration>?,
    internal val errors: Map<String, Int>?
)