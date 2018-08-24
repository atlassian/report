package com.atlassian.performance.tools.report.vmstat

import java.nio.file.Path

/**
 * Converts vmstat log file to csv
 */
internal class VmstatConverter {

    internal fun convertToCsv(
        vmstatLog: Path
    ): Path {
        val vmstatCsv = vmstatLog.parent.resolve("jpt-vmstat.csv")
        vmstatCsv.toFile().createNewFile()

        vmstatLog
            .toFile()
            .inputStream()
            .bufferedReader()
            .use {
                it.lines()
                    .map { it.trim() }
                    .filter { !it.startsWith("procs") }
                    .filter { !it.startsWith("r") }
                    .map { it.replace(Regex(" +"), ",") }
                    .forEach { vmstatCsv.toFile().appendText(it + "\n") }
            }

        return vmstatCsv
    }
}