package com.atlassian.performance.tools.report.drilldown

import com.atlassian.performance.tools.jiraactions.api.w3c.PerformanceNavigationTiming
import com.atlassian.performance.tools.jiraactions.api.w3c.PerformanceResourceTiming
import java.time.Duration
import java.time.Instant

internal class TimeTrain(
    firstStation: Instant,
    private val lastStation: Instant,
    private val timeOrigin: Instant
) {

    private var prevStation: Instant = firstStation

    /**
     * Jump off at the next station
     *
     * @return how much time elapsed since the last station, a linear time segment
     */
    fun jumpOff(nextStation: Instant): Duration {
        /**
         * Some stations are optional, e.g. [PerformanceResourceTiming.workerStart]
         * or might not have happened yet, e.g. before [PerformanceNavigationTiming.loadEventStart]
         * Some stations are parallel and can come in different order in runtime,
         * e.g. last [PerformanceResourceTiming] might come before or after [PerformanceNavigationTiming.loadEventEnd].
         */
        if (nextStation < prevStation) {
            return Duration.ZERO
        }
        val jumpOffStation = minOf(nextStation, lastStation)
        val segment = Duration.between(prevStation, jumpOffStation)
        prevStation = jumpOffStation
        return segment
    }

    fun jumpOff(nextStation: Duration): Duration {
        return jumpOff(timeOrigin + nextStation)
    }
}
