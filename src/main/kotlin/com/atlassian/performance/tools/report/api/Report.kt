package com.atlassian.performance.tools.report.api

interface Report {
    fun generate(): String
}
