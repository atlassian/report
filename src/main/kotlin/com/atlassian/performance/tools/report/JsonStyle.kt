package com.atlassian.performance.tools.report

import java.io.StringWriter
import javax.json.Json
import javax.json.JsonStructure
import javax.json.stream.JsonGenerator

internal class JsonStyle {

    fun prettyPrint(
        json: JsonStructure
    ): String {
        val stringWriter = StringWriter()
        Json
            .createWriterFactory(mapOf(
                JsonGenerator.PRETTY_PRINTING to true
            ))
            .createWriter(stringWriter)
            .use { it.write(json) }
        return stringWriter.toString()
    }
}