package com.atlassian.performance.tools.report

fun Float.toPercentage(decimalPlaces: Int, includeSign: Boolean = true) = toDouble().toPercentage(decimalPlaces, includeSign)

fun Double.toPercentage(decimalPlaces: Int, includeSign: Boolean = true): String {
    return when {
        includeSign -> "%+.${decimalPlaces}f%%".format(this * 100)
        else -> "%.${decimalPlaces}f%%".format(this * 100)
    }
}
