package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.EDIT_ISSUE
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.result.DurationData
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.junit.FailedActionJunitReport
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever

class RelativeNonparametricPerformanceJudgeTest {
    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

    @Test
    fun shouldProduceExpectedFailedActionReportMessage() {
        // given
        val actionToleranceRatio = 0.1f
        val shiftedDistributionRegressionTest = mock(ShiftedDistributionRegressionTest::class.java).apply {
            whenever(isExperimentRegressed(any(Double::class.java))).thenReturn(true)
        }
        val shiftedDistributionRegressionTestProvider = mock(ShiftedDistributionRegressionTestProvider::class.java).apply {
            whenever(
                get(
                    baseline = any(DoubleArray::class.java),
                    experiment = any(DoubleArray::class.java),
                    mwAlpha = any(Double::class.java),
                    ksAlpha = any(Double::class.java)
                )
            ).thenReturn(shiftedDistributionRegressionTest)
        }
        val tested = RelativeNonparametricPerformanceJudge.Builder()
            .significance(0.05)
            .actionMetricsReader {
                mapOf(EDIT_ISSUE.label to mock(DurationData::class.java).apply {
                    whenever(statsValues()).thenReturn(doubleArrayOf())
                })
            }
            .shiftedDistributionRegressionTestProvider(shiftedDistributionRegressionTestProvider)
            .build()
        // when
        val result = tested.judge(
            toleranceRatios = mapOf(EDIT_ISSUE to actionToleranceRatio),
            baselineResult = mock(EdibleResult::class.java),
            experimentResult = mock(EdibleResult::class.java)
        )
        // then
        assertThat(result.reports).hasSize(1)
        assertThat((result.reports[0] as FailedActionJunitReport).toXml(testClassName = "does not matter"))
            .contains("There is a regression in [Full Edit Issue] with 95% confidence level. Regression is larger than allowed +10.00% tolerance")
    }
}
