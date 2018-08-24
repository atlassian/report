package com.atlassian.performance.tools.report.api.parser

import java.io.FileFilter
import java.nio.file.Path

class MergingNodeCountParser {

    private val parser = NodeCountParser()

    fun parse(
        results: Path
    ): Map<String, Int> {
        return results
            .resolve("virtual-users")
            .toFile()
            .listFiles(FileFilter { it.isDirectory })
            .map { it.resolve("test-results") }
            .map { it.resolve("nodes.csv") }
            .filter { it.exists() }
            .map { it.inputStream() }
            .map { it.use { parser.parse(it) } }
            .map { it.entries }
            .flatten()
            .groupingBy { it.key }
            .aggregate { _, accumulator, element, first ->
                if (first) {
                    element.value
                } else {
                    accumulator!! + element.value
                }
            }

    }
}