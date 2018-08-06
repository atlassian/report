package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.report.junit.ExceptionJUnitReport
import com.atlassian.performance.tools.report.junit.SuccessfulJUnitReport

class FailureJudge {

    private val testName = "Failure check"

    fun judge(
        failure: Exception?
    ): Verdict = Verdict(listOf(
        if (failure == null) {
            SuccessfulJUnitReport(testName)
        } else {
            ExceptionJUnitReport(testName, failure)
        }
    ))
}