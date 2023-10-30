package com.atlassian.performance.tools.report.jfr

import java.io.OutputStream

/**
 * We're not using DataOutputStream because [java.io.DataOutputStream.size] is int which will overflow with our data size.
 */
class CountingOutputStream(private val decorated: OutputStream) : OutputStream() {
    var count = 0L
        private set
    var countSinceLastReset = 0L
        private set

    override fun write(b: Int) {
        decorated.write(b)
        count++
        countSinceLastReset++
    }

    override fun write(b: ByteArray) {
        decorated.write(b)
        count += b.size
        countSinceLastReset += b.size
    }

    override fun write(b: ByteArray, offset: Int, length: Int) {
        decorated.write(b, offset, length)
        count += length
        countSinceLastReset += length
    }

    fun resetCount() {
        countSinceLastReset = 0L
    }

    override fun flush() {
        decorated.flush()
    }

    override fun close() {
        decorated.close()
    }
}
