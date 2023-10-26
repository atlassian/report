package com.atlassian.performance.tools.report.jfr

import org.junit.Ignore
import com.atlassian.performance.tools.report.api.result.CompressedResult
import org.junit.Test
import java.io.File

class JfrExecutionEventFilterTest {

    @Test
    @Ignore("WIP")
    fun shouldRewriteJfr() {
        // given
        val zippedInput = File(javaClass.getResource("/profiler-result.zip")!!.toURI())
        val uncompressedInput = CompressedResult.unzip(zippedInput).resolve("profiler-result.jfr")
        // when
        JfrExecutionEventFilter().go(uncompressedInput)
        // then

    }

}
