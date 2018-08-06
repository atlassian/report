package com.atlassian.performance.tools.report

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.InputStream

class NodeCountParser {

    fun parse(
        nodeCountMap: InputStream
    ): Map<String, Int> {
        return CSVParser(
            nodeCountMap.bufferedReader(),
            CSVFormat.DEFAULT
        ).map { record ->
            record.toList()
        }.map { fields ->
            val node = fields[0]
            val count = fields[1].toInt()
            node to count
        }.toMap()
    }
}