package com.atlassian.performance.tools.report.jfr

import java.io.InputStream
import java.io.OutputStream

object VarInt {

    /**
     * Impl copied from [jdk.jfr.internal.EventWriter.putUncheckedLong]
     */
    fun write(value: Long, stream: OutputStream) {
        fun putUncheckedLong(v: Long) {
            var v = v
            if (v and 0x7FL.inv() == 0L) {
                putUncheckedByte(v.toByte()) // 0-6
                return
            }
            putUncheckedByte((v or 0x80L).toByte()) // 0-6
            v = v ushr 7
            if (v and 0x7FL.inv() == 0L) {
                putUncheckedByte(v.toByte()) // 7-13
                return
            }
            putUncheckedByte((v or 0x80L).toByte()) // 7-13
            v = v ushr 7
            if (v and 0x7FL.inv() == 0L) {
                putUncheckedByte(v.toByte()) // 14-20
                return
            }
            putUncheckedByte((v or 0x80L).toByte()) // 14-20
            v = v ushr 7
            if (v and 0x7FL.inv() == 0L) {
                putUncheckedByte(v.toByte()) // 21-27
                return
            }
            putUncheckedByte((v or 0x80L).toByte()) // 21-27
            v = v ushr 7
            if (v and 0x7FL.inv() == 0L) {
                putUncheckedByte(v.toByte()) // 28-34
                return
            }
            putUncheckedByte((v or 0x80L).toByte()) // 28-34
            v = v ushr 7
            if (v and 0x7FL.inv() == 0L) {
                putUncheckedByte(v.toByte()) // 35-41
                return
            }
            putUncheckedByte((v or 0x80L).toByte()) // 35-41
            v = v ushr 7
            if (v and 0x7FL.inv() == 0L) {
                putUncheckedByte(v.toByte()) // 42-48
                return
            }
            putUncheckedByte((v or 0x80L).toByte()) // 42-48
            v = v ushr 7
            if (v and 0x7FL.inv() == 0L) {
                putUncheckedByte(v.toByte()) // 49-55
                return
            }
            putUncheckedByte((v or 0x80L).toByte()) // 49-55
            putUncheckedByte((v ushr 7).toByte()) // 56-63, last byte as is.
        }
    }

    fun read(stream: InputStream): Long {
        return 0L
    }
}
