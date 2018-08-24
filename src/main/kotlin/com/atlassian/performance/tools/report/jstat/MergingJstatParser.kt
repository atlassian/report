package com.atlassian.performance.tools.report.jstat

import com.atlassian.performance.tools.infrastructure.jvm.Jstat
import com.atlassian.performance.tools.infrastructure.metric.SystemMetric
import java.io.FileFilter
import java.nio.file.Path

internal class MergingJstatParser {

    private val converter = JstatConverter()
    private val parser = JstatGcutilParser()

    fun parse(
        results: Path
    ): List<SystemMetric> {
        return results
            .toFile()
            .listFiles(FileFilter { it.isDirectory })
            .map { it.resolve(Jstat.LOG_FILE_NAME) }
            .filter { it.exists() }
            .map { it.toPath() }
            .map { converter.convertToCsv(it) }
            .map { it.toFile() }
            .map { csv -> csv.inputStream().use { parser.parse(it, csv.parentFile.name) } }
            .flatten()
    }
}