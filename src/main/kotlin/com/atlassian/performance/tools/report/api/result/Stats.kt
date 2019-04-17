package com.atlassian.performance.tools.report.api.result

import java.time.Duration

/**
 * Group of statistical parameters of test results
 */
interface Stats {
    /**
     * Name of the test cohort
     */
    val cohort: String
    /**
     * [Sample size](https://en.wikipedia.org/wiki/Statistical_sample) per action
     */
    val sampleSizes: Map<String, Long>
    /**
     * [Locations](https://en.wikipedia.org/wiki/Location_parameter) per action
     */
    val locations: Map<String, Duration>
    /**
     * [Dispersions](https://en.wikipedia.org/wiki/Statistical_dispersion) per action
     */
    val dispersions: Map<String, Duration>
    /**
     * Number of errors per action
     */
    val errors: Map<String, Int>
}