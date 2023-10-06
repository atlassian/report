package com.atlassian.performance.tools.report.chart

import com.atlassian.performance.tools.report.JsonProviderSingleton.JSON
import javax.json.JsonObject

internal data class ChartAxis(
    val id: String,
    val text: String
) {
    fun toJson(): JsonObject {
        return JSON.createObjectBuilder()
            .add("id", id)
            .add("text", text)
            .build()
    }
}
