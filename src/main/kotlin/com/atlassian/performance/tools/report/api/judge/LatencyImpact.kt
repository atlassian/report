package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.VIEW_ISSUE
import java.time.Duration

/**
 * @param action subject of the impact, e.g. this is how latency of [VIEW_ISSUE] was impacted
 * @param relative relative size of the impact, e.g. 0.15 is +15% latency or -0.04 is -4% latency
 * @param absolute absolute size of the impact, e.g +300 ms or -50 ms
 * @param conclusive if true, then [relative] and [absolute] sizes should be considered a signal rather than noise
 */
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
