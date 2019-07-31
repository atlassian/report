package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.junit.JUnitReport

/**
 * Action aware report
 *
 * @property action Null if no Action has been associated with this report.
 * @property critical False if the failure in this report is expected sometimes as a part of a normal flow, True otherwise.
 */
class ActionReport(
    internal val report: JUnitReport,
    val action: ActionType<*>?,
    val critical: Boolean
) {

    val successful = report.successful
}
