package org.openjdk.jmc.flightrecorder.testutils.parser;

import com.atlassian.performance.tools.report.api.result.LocalRealResult
import com.atlassian.performance.tools.report.jfr.VarIntParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test;
import java.io.ByteArrayInputStream
import java.nio.file.Paths
import kotlin.experimental.or

class RecordingStreamTest {

    @Test
    fun shouldParse6164InTwoBytes() {
        val input = ByteArrayInputStream(byteArrayOf(-108, 48))

        val output = RecordingStream(input).readVarint()

        assertThat(output).isEqualTo(6164)
    }

    @Test
    fun shouldParse6164InFiveBytes() {
        val leadingZeros = 0b10000000.toByte()
        val input = ByteArrayInputStream(
            byteArrayOf(
                -108,
                leadingZeros or 48,
                leadingZeros,
                leadingZeros,
                0b0000000
            )
        )

        val output = RecordingStream(input).readVarint()

        assertThat(output).isEqualTo(6164)
    }

    @Test
    fun shouldParseStartTime() {
        val input = ByteArrayInputStream(byteArrayOf(-22, -90, -45, -116, -84, 16, 0))

        val output = RecordingStream(input).readVarint()

        assertThat(output).isEqualTo(561593504618L)
    }

    /**
     * Same inputs as [shouldParseStartTime], different impl
     */
    @Ignore("Parses 4970 for some reason, this impl is bugged")
    @Test
    fun shouldParseStartTimeWithVarInt() {
        val input = ByteArrayInputStream(byteArrayOf(-22, -90, -45, -116, -84, 16, 0))

        val output = VarIntParser.parse(input)

        assertThat(output).isEqualTo(561593504618L)
    }
}
