package com.atlassian.performance.tools.report.jfr

import java.io.OutputStream

/**
 * We're not using DataOutputStream because [java.io.DataOutputStream.size] is int which will overflow with our data size.
 * Moreover, we're keeping two counters. One for absolute position, second for resettable offset.
 */
class CountingOutputStream(private val decorated: OutputStream) : OutputStream() {
    var position = 0L
        private set
    var offsetSinceLastReset = 0L
        private set

    override fun write(b: Int) {
        decorated.write(b)
        position++
        offsetSinceLastReset++
    }

    override fun write(b: ByteArray) {
        decorated.write(b)
        position += b.size
        offsetSinceLastReset += b.size
    }

    override fun write(b: ByteArray, offset: Int, length: Int) {
        decorated.write(b, offset, length)
        position += length
        offsetSinceLastReset += length
    }

    fun resetOffset() {
        offsetSinceLastReset = 0L
    }

    override fun flush() {
        decorated.flush()
    }

    override fun close() {
        decorated.close()
    }
}
