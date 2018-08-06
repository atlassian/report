package com.atlassian.performance.tools.report

import java.io.StringWriter
import javax.json.Json
import javax.json.JsonObject
import javax.json.stream.JsonGenerator

class JsonStyle {

    fun prettyPrint(
        json: JsonObject
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