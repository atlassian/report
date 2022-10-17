package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.report.api.ShiftedDistributionRegressionTest
import com.atlassian.performance.tools.report.api.result.DurationData
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.junit.FailedActionJunitReport
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.time.Duration
import java.time.Instant
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

    @Test
    fun shouldVerdictHaveFailedReports() {
        // given
        fun generateBaseline(actionTypes: List<ActionType<*>>): List<ActionMetric> {
            return LongRange(0, 120).map { index ->
                actionTypes.map { actionType ->
                    ActionMetric.Builder(
                        actionType.label,
                        ActionResult.OK,
                        Duration.ofMillis(10).plusMillis(index * 10),
                        Instant.now().minusSeconds(index)
                    ).build()
                }
            }.flatten()
        }

        fun generateExperiment(actionTypes: List<ActionType<*>>): List<ActionMetric> {
            return LongRange(0, 120).map { index ->
                actionTypes.map { actionType ->
                    ActionMetric.Builder(
                        actionType.label,
                        ActionResult.OK,
                        Duration.ofSeconds(100),
                        Instant.now().minusSeconds(index)
                    ).build()
                }
            }.flatten()
        }

        val actionTypes: List<ActionType<*>> = listOf(EDIT_ISSUE, ADD_COMMENT)
        val zeroToleranceRatios = actionTypes.associate { it to 0f }.toMap()
        val baselineMetrics = generateBaseline(actionTypes)
        val experimentMetrics = generateExperiment(actionTypes)

        // when
        val result = RelativeNonparametricPerformanceJudge.Builder().build()
            .judge(
                toleranceRatios = zeroToleranceRatios,
                baselineResult = EdibleResult.Builder("baseline mock")
                    .actionMetrics(baselineMetrics)
                    .build(),
                experimentResult = EdibleResult.Builder("experiment mock")
                    .actionMetrics(experimentMetrics)
                    .build()
            )
        // then
        assertThat(result.reports).hasSize(2)
        assertThat(result.reports).allSatisfy { !it.successful }
    }
}
