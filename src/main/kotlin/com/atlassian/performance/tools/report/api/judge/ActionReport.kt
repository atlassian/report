package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.junit.JUnitReport

/**
 * Action aware report
 *
 * @property action Action associated with this report.
 * @property nonExceptional True if the failure in this report is expected sometimes as a part of a normal flow, false otherwise.
 */
interface ActionReport : JUnitReport {

    val action: ActionType<*>
    val nonExceptional: Boolean
}
