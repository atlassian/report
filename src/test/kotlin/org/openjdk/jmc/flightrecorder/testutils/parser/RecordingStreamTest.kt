package org.openjdk.jmc.flightrecorder.testutils.parser;

import com.atlassian.performance.tools.report.jfr.VarInt
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test;
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
    fun shouldWriteAndRead() {
        // given
        val bytes = ByteArrayOutputStream().let {
            VarInt.write(6164, it)
            VarInt.write(0, it)
            it.toByteArray()
        }

        // when
        val stream = RecordingStream(ByteArrayInputStream(bytes))
        val first = stream.readVarint()
        val second = stream.readVarint()

        // then
        assertThat(first).isEqualTo(6164)
        assertThat(second).isEqualTo(0)
        assertThat(stream.position()).isEqualTo(bytes.size.toLong())
    }
}
