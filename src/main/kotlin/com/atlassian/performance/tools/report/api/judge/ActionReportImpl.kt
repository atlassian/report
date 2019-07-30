package com.atlassian.performance.tools.report.api.judge

import com.atlassian.performance.tools.jiraactions.api.ActionType
import com.atlassian.performance.tools.report.api.junit.JUnitReport
import java.nio.file.Path

internal class ActionReportImpl(
    internal val report: JUnitReport,
    override val action: ActionType<*>,
    override val nonExceptional: Boolean
) : ActionReport {

    override val testName = report.testName
    override val successful = report.successful

    override fun toXml(testClassName: String) = report.toXml(testClassName)
    override fun dump(testClassName: String, path: Path) = report.dump(testClassName, path)
}
