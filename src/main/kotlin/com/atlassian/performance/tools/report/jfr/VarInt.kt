package com.atlassian.performance.tools.report.jfr

import java.io.InputStream
import java.io.OutputStream

object VarInt {

    /**
     * Impl copied from [jdk.jfr.internal.EventWriter.putUncheckedLong]
     */
    fun write(value: Long, stream: OutputStream) {
        var v = value
        if (v and 0x7FL.inv() == 0L) {
            stream.write(v.toInt()) // 0-6
            return
        }
        stream.write((v or 0x80L).toInt()) // 0-6
        v = v ushr 7
        if (v and 0x7FL.inv() == 0L) {
            stream.write(v.toInt()) // 7-13
            return
        }
        stream.write((v or 0x80L).toInt()) // 7-13
        v = v ushr 7
        if (v and 0x7FL.inv() == 0L) {
            stream.write(v.toInt()) // 14-20
            return
        }
        stream.write((v or 0x80L).toInt()) // 14-20
        v = v ushr 7
        if (v and 0x7FL.inv() == 0L) {
            stream.write(v.toInt()) // 21-27
            return
        }
        stream.write((v or 0x80L).toInt()) // 21-27
        v = v ushr 7
        if (v and 0x7FL.inv() == 0L) {
            stream.write(v.toInt()) // 28-34
            return
        }
        stream.write((v or 0x80L).toInt()) // 28-34
        v = v ushr 7
        if (v and 0x7FL.inv() == 0L) {
            stream.write(v.toInt()) // 35-41
            return
        }
        stream.write((v or 0x80L).toInt()) // 35-41
        v = v ushr 7
        if (v and 0x7FL.inv() == 0L) {
            stream.write(v.toInt()) // 42-48
            return
        }
        stream.write((v or 0x80L).toInt()) // 42-48
        v = v ushr 7
        if (v and 0x7FL.inv() == 0L) {
            stream.write(v.toInt()) // 49-55
            return
        }
        stream.write((v or 0x80L).toInt()) // 49-55
        stream.write((v ushr 7).toInt()) // 56-63, last byte as is.
    }

}
