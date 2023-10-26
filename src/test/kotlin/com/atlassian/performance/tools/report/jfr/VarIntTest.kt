package com.atlassian.performance.tools.report.jfr

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream

class VarIntTest {


    // i    readValue   readValue & 0x7F    (readValue & 0x7F) << (7 * i)   value
// 0    10010100    ?0010100                                 00010100                              ?0010100
// 1    10110000    ?0110000                        00011000_00000000                     00011000_00010100
// 2    10000000    00000000               00000000_00000000_00000000            00000000_00011000_00010100
// 3    10000000    00000000                                            00000000_00000000_00011000_00010100
// 4    00000000    00000000                                                              00011000_10010100


    @Test
    fun shouldWrite() {
        val stream = ByteArrayOutputStream(8)
        VarInt.write(0b00011000_10010100, stream)

        val writtenBytes: ByteArray = stream.toByteArray()
        assertThat(writtenBytes).isEqualTo(
            byteArrayOf(
                0b10010100.toByte(),
                0b10110000.toByte(),
                0b10000000.toByte(),
                0b10000000.toByte(),
                0b00000000.toByte()
            )
        )
    }
}

