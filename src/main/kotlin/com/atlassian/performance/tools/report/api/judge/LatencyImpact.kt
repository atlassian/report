package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import java.time.Duration

class LatencyImpact(
    val action: ActionType<*>,
    val relative: Double,
    val absolute: Duration,
    val regressionDetected: Boolean
)
