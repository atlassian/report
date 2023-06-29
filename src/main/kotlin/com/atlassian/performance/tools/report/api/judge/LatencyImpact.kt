package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.VIEW_ISSUE
import java.time.Duration

/**
 * @param action subject of the impact, e.g. this is how latency of [VIEW_ISSUE] was impacted
 * @param relative relative size of the impact, e.g. 0.15 is +15% latency or -0.04 is -4% latency
 * @param absolute absolute size of the impact, e.g +300 ms or -50 ms
 * @param signal if true, then [relative] and [absolute] are a real signal; otherwise they're [noise]
 *
 * Terminology: [signal and noise](https://en.wikipedia.org/wiki/Signal-to-noise_ratio)
 */
class LatencyImpact private constructor(
    val action: ActionType<*>,
    val relative: Double,
    val absolute: Duration,
    val signal: Boolean
) {

    /**
     * If true, then it's a real regression with given [relative] and [absolute] size.
     * If false, then it's an [improvement] or [noise].
     */
    val regression: Boolean = relative > 0.0 && signal

    /**
     * If true, then it's a real improvement with given [relative] and [absolute] size.
     * If false, then it's an [regression] or [noise].
     */
    val improvement: Boolean = relative < 0.0 && signal

    /**
     * If true, then it's neither [regression] nor [improvement]. It's just noise with [relative] and [absolute] size.
     * If false, then it's either [regression] or [improvement]. It's a [signal].
     */
    val noise = !signal

    class Builder(
        private var action: ActionType<*>,
        private var relative: Double,
        private var absolute: Duration
    ) {
        private var signal: Boolean = false

        fun action(action: ActionType<*>) = apply { this.action = action }
        fun relative(relative: Double) = apply { this.relative = relative }
        fun absolute(absolute: Duration) = apply { this.absolute = absolute }
        fun signal(signal: Boolean) = apply { this.signal = signal }
        fun signal() = signal(true)
        fun noise(noise: Boolean) = signal(!noise)
        fun noise() = noise(true)

        fun build() = LatencyImpact(action, relative, absolute, signal)
    }
}
