package com.atlassian.performance.tools.report.chart.waterfall

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UtilsTest {

    @Test
    fun shouldPrettyPrintUri() {
        val utils = Utils()
        val expectedPath = "somePath"
        val uri = "https://www.google.com/$expectedPath"

        val actualPath = utils.prettyPrint(uri)

        assertThat(actualPath).isEqualTo(expectedPath)
    }

    @Test
    fun shouldPrettyPrintUriForLongPath() {
        val utils = Utils()
        val longPath = "someVeryVeryVeyVeryLongPathWhichIsReallyReallyReallyLong"
        val expectedPath = "someVeryVeryVeyVeryLongPath..."
        val uri = "https://www.google.com/$longPath"

        val actualPath = utils.prettyPrint(uri)

        assertThat(actualPath).isEqualTo(expectedPath)
    }

    @Test
    fun shouldPrettyPrintUriWithoutPath() {
        val utils = Utils()
        val schemeSpecificPart = "svg+xml;base64,PD94bWwgdmVy"
        val uri = "data:image/$schemeSpecificPart"

        val actualPath = utils.prettyPrint(uri)

        assertThat(actualPath).isEqualTo(schemeSpecificPart)
    }
}
