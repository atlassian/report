package com.atlassian.performance.tools.report

fun Float.toPercentage(decimalPlaces: Int): String = "%+.${decimalPlaces}f%%".format(this * 100)
fun Double.toPercentage(decimalPlaces: Int): String = "%+.${decimalPlaces}f%%".format(this * 100)