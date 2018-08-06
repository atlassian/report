package com.atlassian.performance.tools.report

import javax.json.Json
import javax.json.JsonObject

data class ChartAxis(
    val id: String,
    val text: String
) {
    fun toJson(): JsonObject {
        return Json.createObjectBuilder()
            .add("id", id)
            .add("text", text)
            .build()
    }
}