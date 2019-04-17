package com.atlassian.performance.tools.report.jstat

import java.nio.file.Files
import java.nio.file.Path

/**
 * NOTE: You are advised not to write scripts to parse jstat's output since the format may change in future releases.
 * If you choose to write scripts that parse jstat output, expect to modify them for future releases of this tool.
 *
 * @see <a href="https://docs.oracle.com/javase/7/docs/technotes/tools/share/jstat.html">jstat docs</a>
 */
internal class JstatConverter {

    internal fun convertToCsv(
        jstat: Path
    ): Path {
        val jstatCsvName = "${jstat.fileName.toString().substringBeforeLast(".")}.csv"
        val jstatCsv = jstat.parent.resolve(jstatCsvName)
        if (jstatCsv.toFile().exists()) {
            Files.delete(jstatCsv)
        }
        jstat
            .toFile()
            .inputStream()
            .bufferedReader()
            .use {
                val header = extractHeader(it.readLine())
                jstatCsv.toFile().appendText("$header\n")
                it.lines()
                    .map { toCsvLine(it) }
                    .forEach { jstatCsv.toFile().appendText("$it\n") }
            }

        return jstatCsv
    }

    private fun toCsvLine(logLine: String): String {
        return logLine.replace(Regex(" +"), ",")
            .replace(",-", ",0")
    }

    private fun extractHeader(line: String): String {
        val columns = toCsvLine(line).split(",")
        val header = listOf(JstatGcutilHeader.DATE.toString()) + columns.subList(1, columns.size)
        return header
            .filter { it.isNotEmpty() }
            .joinToString(",").toUpperCase()
    }

}