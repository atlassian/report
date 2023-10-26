package com.atlassian.performance.tools.report.jfr

import org.junit.Ignore
import org.junit.Test
import java.io.File

class JfrExecutionEventFilterTest {

    @Test
    @Ignore("WIP")
    fun shouldRewriteJfr() {
        val zippedInput = File(javaClass.getResource("/profiler-result.zip")!!.toURI())
        val input = unzip(zippedInput)
        JfrExecutionEventFilter().go(input.toPath())
    }

    fun unzip(file: File): File {
        TODO()
    }
}
