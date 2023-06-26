package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import java.time.Duration

class LatencyImpact private constructor(
    val action: ActionType<*>,
    val relative: Double,
    val absolute: Duration,
    val conclusive: Boolean
) {

    val regression: Boolean = relative > 0.0 && conclusive
    val improvement: Boolean = relative < 0.0 && conclusive

    class Builder(
        private var action: ActionType<*>,
        private var relative: Double,
        private var absolute: Duration
    ) {
        private var conclusive: Boolean = false

        fun action(action: ActionType<*>) = apply { this.action = action }
        fun relative(relative: Double) = apply { this.relative = relative }
        fun absolute(absolute: Duration) = apply { this.absolute = absolute }
        fun conclusive(conclusive: Boolean) = apply { this.conclusive = conclusive }

        fun build() = LatencyImpact(action, relative, absolute, conclusive)
    }
}
