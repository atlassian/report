package com.atlassian.performance.tools.report.jstat

import java.nio.file.Path

internal class JstatConverter {

    internal fun convertToCsv(
        jstat: Path
    ): Path {
        val jstatCsvName = "${jstat.fileName.toString().substringBeforeLast(".")}.csv"
        val jstatCsv = jstat.parent.resolve(jstatCsvName)
        jstatCsv.toFile().createNewFile()

        jstat
            .toFile()
            .inputStream()
            .bufferedReader()
            .use {
                it.lines()
                    .skip(1)
                    .map { it.replace(Regex(" +"), ",") }
                    .forEach { jstatCsv.toFile().appendText(it + "\n") }
            }

        return jstatCsv
    }
}