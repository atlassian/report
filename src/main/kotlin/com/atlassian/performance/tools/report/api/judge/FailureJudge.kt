package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.report.api.junit.ExceptionJUnitReport
import com.atlassian.performance.tools.report.api.junit.SuccessfulJUnitReport

class FailureJudge {

    private val testName = "Failure check"

    fun judge(
        failure: Exception?
    ): Verdict = Verdict
        .Builder(
            reports = listOf(
                if (failure == null) {
                    SuccessfulJUnitReport(testName)
                } else {
                    ExceptionJUnitReport(testName, failure)
                }
            )
        )
        .build()
}
