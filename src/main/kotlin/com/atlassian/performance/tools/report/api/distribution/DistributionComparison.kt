package com.atlassian.performance.tools.report.api.distribution

class DistributionComparison(
    val experimentRelativeChange: Double,
    val experimentAbsoluteChange: Double,
    val isExperimentRegressed: Boolean,
    val isExperimentImproved: Boolean
) {

    init {
        if (isExperimentImproved && isExperimentRegressed) {
            throw IllegalArgumentException("Experiment can't be both regressed and improved at the same time")
        }
    }

    fun hasImpact() = isExperimentRegressed || isExperimentImproved

}
