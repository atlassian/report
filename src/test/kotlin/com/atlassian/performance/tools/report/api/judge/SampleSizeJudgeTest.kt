package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.jiraactions.api.VIEW_ISSUE
import com.atlassian.performance.tools.report.api.SampleSizeCriteria
import com.atlassian.performance.tools.report.api.result.InteractionStats
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SampleSizeJudgeTest {

    @Rule
    @JvmField
    val workspace = TemporaryFolder()

    @Test
    fun shouldJudgeWhenSampleSizeIsMissing() {
        // given
        val sampleSizeCriteria = mapOf<ActionType<*>, SampleSizeCriteria>(VIEW_ISSUE to SampleSizeCriteria(0))
        val noSampleSizes = mapOf<String, Long>()
        val actionStats = InteractionStats("someCohort", noSampleSizes, null, null, null)

        // when
        val verdict = SampleSizeJudge().judge(actionStats, sampleSizeCriteria)

        // then
        verdict.assertAccepted("shouldJudgeWhenSampleSizeIsMissing", workspace.newFolder().toPath(), expectedReportCount = 1)
    }

}
