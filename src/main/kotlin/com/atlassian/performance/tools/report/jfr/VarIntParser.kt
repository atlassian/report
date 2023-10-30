package com.atlassian.performance.tools.report.jfr

import java.io.InputStream

object VarIntParser {
    /**
     * Based on [jdk.jfr.internal.consumer.RecordingInput.readLong]
     */
    fun parse(byteStream: InputStream): Long {
        // can be optimized by branching checks, but will do for now
        val b0: Byte = byteStream.read().toByte()
        var ret = b0.toLong() and 0x7FL
        if (b0 >= 0) {
            return ret
        }
        val b1: Int = byteStream.read()
        ret += b1.toLong() and 0x7FL shl 7
        if (b1 >= 0) {
            return ret
        }
        val b2: Int = byteStream.read()
        ret += b2.toLong() and 0x7FL shl 14
        if (b2 >= 0) {
            return ret
        }
        val b3: Int = byteStream.read()
        ret += b3.toLong() and 0x7FL shl 21
        if (b3 >= 0) {
            return ret
        }
        val b4: Int = byteStream.read()
        ret += b4.toLong() and 0x7FL shl 28
        if (b4 >= 0) {
            return ret
        }
        val b5: Int = byteStream.read()
        ret += b5.toLong() and 0x7FL shl 35
        if (b5 >= 0) {
            return ret
        }
        val b6: Int = byteStream.read()
        ret += b6.toLong() and 0x7FL shl 42
        if (b6 >= 0) {
            return ret
        }
        val b7: Int = byteStream.read()
        ret += b7.toLong() and 0x7FL shl 49
        if (b7 >= 0) {
            return ret
        }
        val b8: Int = byteStream.read() // read last byte raw
        return ret + ((b8 and 0XFF).toLong() shl 56)
    }

}
