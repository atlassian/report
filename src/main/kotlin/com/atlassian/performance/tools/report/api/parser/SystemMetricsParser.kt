package com.atlassian.performance.tools.report.api.parser

import com.atlassian.performance.tools.infrastructure.metric.SystemMetric
import com.atlassian.performance.tools.report.jstat.MergingJstatParser
import com.atlassian.performance.tools.report.vmstat.MergingVmstatParser
import java.nio.file.Path

class SystemMetricsParser {
    private val vmstatParser = MergingVmstatParser()
    private val jstatParser = MergingJstatParser()

    fun parse(
        results: Path
    ): List<SystemMetric> {
        return vmstatParser.parse(results) + jstatParser.parse(results)
    }
}