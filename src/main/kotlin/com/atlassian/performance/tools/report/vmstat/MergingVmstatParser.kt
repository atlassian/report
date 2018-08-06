package com.atlassian.performance.tools.report.vmstat

import com.atlassian.performance.tools.infrastructure.metric.SystemMetric
import com.atlassian.performance.tools.infrastructure.os.Vmstat
import java.io.FileFilter
import java.nio.file.Path

class MergingVmstatParser {

    private val converter = VmstatConverter()
    private val parser = VmstatParser()

    fun parse(
        results: Path
    ): List<SystemMetric> {
        return results
            .toFile()
            .listFiles(FileFilter { it.isDirectory })
            .map { it.resolve(Vmstat.LOG_FILE_NAME) }
            .filter { it.exists() }
            .map { it.toPath() }
            .map { converter.convertToCsv(it) }
            .map { it.toFile() }
            .map { csv -> csv.inputStream().use { parser.parse(it, csv.parentFile.name) } }
            .flatten()
    }
}