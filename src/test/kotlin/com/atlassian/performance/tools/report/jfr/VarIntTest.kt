package com.atlassian.performance.tools.report.jfr

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Test input data was taken from [org.openjdk.jmc.flightrecorder.testutils.parser.RecordingStream.readVarint] debugging on real JFR file.
 */
class VarIntTest {

    @Test
    fun shouldWrite0() {
        val stream = ByteArrayOutputStream(8)
        VarInt.write(0, stream)

        val writtenBytes: ByteArray = stream.toByteArray()
        assertThat(writtenBytes).isEqualTo(
            byteArrayOf(0)
        )
    }

    @Test
    fun shouldWrite561593504618() {
        val stream = ByteArrayOutputStream(8)
        VarInt.write(561593504618, stream)

        val writtenBytes: ByteArray = stream.toByteArray()
        assertThat(writtenBytes).isEqualTo(
            byteArrayOf(
                234.toByte(), 166.toByte(), 211.toByte(), 140.toByte(), 172.toByte(), 16.toByte()
            )
        )
    }

    @Test
    fun shouldWrite2147483647() {
        val stream = ByteArrayOutputStream(8)
        VarInt.write(2147483647, stream)

        val writtenBytes: ByteArray = stream.toByteArray()
        assertThat(writtenBytes).isEqualTo(
            byteArrayOf(
                255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 7.toByte()
            )
        )
    }
}

