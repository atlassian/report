package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.infrastructure.virtualusers.LoadProfile
import com.atlassian.performance.tools.jiraactions.ActionType
import java.time.Duration

data class PerformanceCriteria(
    val actionCriteria: Map<ActionType<*>, Criteria>,
    val loadProfile: LoadProfile,
    val maxVirtualUsersImbalance: Int = 8,
    val maxInactiveVirtualUsers: Int = 1,
    val nodes: Int = 1
) {
    fun getCenterCriteria() = actionCriteria.mapValues { (_, criteria) -> criteria.centerToleranceRatio }

    fun getDispersionCriteria() = actionCriteria.mapValues { (_, criteria) -> criteria.maxDispersionDifference }

    fun getSampleSizeCriteria() = actionCriteria.mapValues { (_, criteria) -> criteria.sampleSizeCriteria }

    fun getErrorCriteria() = actionCriteria.mapValues { (_, criteria) -> criteria.errorCriteria }
}

data class Criteria(
    val centerToleranceRatio: Float,
    val maxDispersionDifference: Duration,
    val sampleSizeCriteria: SampleSizeCriteria,
    val errorCriteria: ErrorCriteria,
    val outlierTrimming: OutlierTrimming
) {
    @JvmOverloads
    constructor(
        toleranceRatio: Float = Float.NaN,
        minimumSampleSize: Long,
        acceptableErrorCount: Int = 0,
        maxDispersionDifference : Duration = Duration.ofMillis(500),
        outlierTrimming : OutlierTrimming = OutlierTrimming(
            lowerTrim = 0.01,
            upperTrim = 0.99
        )
    ) : this (
        centerToleranceRatio = toleranceRatio,
        maxDispersionDifference = maxDispersionDifference,
        sampleSizeCriteria = SampleSizeCriteria(minimumSampleSize),
        errorCriteria = ErrorCriteria(acceptableErrorCount),
        outlierTrimming = outlierTrimming
    )
}

data class SampleSizeCriteria(
    val minimumSampleSize: Long
)

data class ErrorCriteria(
    val acceptableErrorCount: Int
)