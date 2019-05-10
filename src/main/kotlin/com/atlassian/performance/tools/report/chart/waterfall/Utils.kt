package com.atlassian.performance.tools.report.chart.waterfall

import com.atlassian.performance.tools.report.JsonStyle
import java.text.DecimalFormat
import javax.json.JsonObject

internal class Utils {

    fun toHumanReadableSize(size: Long): String {
        if (size <= 0)
            return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()

        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    fun prettyPrint(json: JsonObject): String = JsonStyle().prettyPrint(json)

    fun prettyPrint(address: String): String {
        val label = address.substringAfterLast('/')
        val maxLength = 30
        return if (label.length > maxLength) {
            label.substring(0, maxLength - 3) + "..."
        } else {
            label
        }
    }
}
