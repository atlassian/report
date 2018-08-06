package com.atlassian.performance.tools.report

import java.math.BigDecimal

interface Point<X> {

    val x: X
    val y: BigDecimal
    fun labelX(): String
}