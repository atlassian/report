package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.report.api.result.EdibleResult
import com.atlassian.performance.tools.report.junit.FailedActionJunitReport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RelativeNonparametricPerformanceJudgeTest {

    private fun generateBaseline(actionTypes: List<ActionType<*>>): List<ActionMetric> {
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

    private fun generateExperiment(actionTypes: List<ActionType<*>>): List<ActionMetric> {
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

    private val actionTypes: List<ActionType<*>> = listOf(EDIT_ISSUE, ADD_COMMENT)
    private val zeroToleranceRatios = actionTypes.associate { it to 0.1f }.toMap()
    private val baselineMetrics = generateBaseline(actionTypes)
    private val experimentMetrics = generateExperiment(actionTypes)
    private val tested = RelativeNonparametricPerformanceJudge()
        .judge(
            toleranceRatios = zeroToleranceRatios,
            baselineResult = EdibleResult.Builder("baseline mock")
                .actionMetrics(baselineMetrics)
                .build(),
            experimentResult = EdibleResult.Builder("experiment mock")
                .actionMetrics(experimentMetrics)
                .build()
        )

    @Test
    fun shouldVerdictHaveFailedReports() {
        assertThat(tested.reports).hasSize(2)
        assertThat(tested.reports).allSatisfy { !it.successful }
    }

    @Test
    fun shouldFailedReportXmlHaveExpectedMessage() {
        assertThat(tested.reports).hasSize(2)
        tested.reports.first().let { report ->
            assertThat(report).isInstanceOf(FailedActionJunitReport::class.java)
            assertThat((report as FailedActionJunitReport).toXml(testClassName = "does not matter"))
                .contains("There is a regression in [Full Edit Issue] with 95% confidence level. Regression is larger than allowed +10.00% tolerance")
        }
    }

    @Test
    fun shouldReportLatencyImpacts() {
        val impacts = mutableListOf<LatencyImpact>()

        RelativeNonparametricPerformanceJudge.Builder()
            .handleLatencyImpact(impacts::add)
            .build()
            .judge(
                toleranceRatios = zeroToleranceRatios,
                baselineResult = EdibleResult.Builder("baseline mock")
                    .actionMetrics(baselineMetrics)
                    .build(),
                experimentResult = EdibleResult.Builder("experiment mock")
                    .actionMetrics(experimentMetrics)
                    .build()
            )

        assertThat(impacts).isNotEmpty()
        impacts.forEach { impact ->
            assertThat(actionTypes).contains(impact.action)
            assertThat(impact.relative).isBetween(-0.01f, 0.34f)
            assertThat(impact.absolute).isBetween(Duration.ofMillis(-40), Duration.ofMillis(600))
        }
        assertThat(impacts).hasOnlyOneElementSatisfying { regression ->
            assertThat(regression.action).isEqualTo(EDIT_ISSUE)
            assertThat(regression.relative).isCloseTo(0.33f, Offset.offset(0.01f))
            assertThat(regression.regressionDetected).isTrue()
        }
    }
}
