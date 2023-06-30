package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.VIEW_ISSUE
import java.time.Duration

/**
 * @param action subject of the impact, e.g. this is how latency of [VIEW_ISSUE] was impacted
 * @param relativeDiff relative size of the impact, e.g. 0.15 is +15% latency or -0.04 is -4% latency
 * @param absoluteDiff absolute size of the impact, e.g +300 ms or -50 ms
 * @param relevant if true, then [relativeDiff] and [absoluteDiff] are a big enough to matter; otherwise it's [irrelevant]
 */
class LatencyImpact private constructor(
    val action: ActionType<*>,
    val relativeDiff: Double,
    val absoluteDiff: Duration,
    val relevant: Boolean
) {

    /**
     * If true, then it's a real regression with given [relativeDiff] and [absoluteDiff] size.
     * If false, then it's an [improvement] or [irrelevant].
     */
    val regression: Boolean = relativeDiff > 0.0 && relevant

    /**
     * If true, then it's a real improvement with given [relativeDiff] and [absoluteDiff] size.
     * If false, then it's an [regression] or [irrelevant].
     */
    val improvement: Boolean = relativeDiff < 0.0 && relevant

    /**
     * If true, then it's neither [regression] nor [improvement].
     * The [relativeDiff] and [absoluteDiff] can be non-zero, but it's judged to be too small to matter.
     * If false, then it's either [regression] or [improvement]. It's [relevant].
     */
    val irrelevant = !relevant

    class Builder(
        private var action: ActionType<*>,
        private var relativeDiff: Double,
        private var absoluteDiff: Duration
    ) {
        private var relevant: Boolean = true

        fun action(action: ActionType<*>) = apply { this.action = action }
        fun relativeDiff(relativeDiff: Double) = apply { this.relativeDiff = relativeDiff }
        fun absoluteDiff(absoluteDiff: Duration) = apply { this.absoluteDiff = absoluteDiff }
        fun relevant(relevant: Boolean) = apply { this.relevant = relevant }
        fun relevant() = relevant(true)
        fun irrelevant() = relevant(false)

        fun build() = LatencyImpact(action, relativeDiff, absoluteDiff, relevant)
    }
}
