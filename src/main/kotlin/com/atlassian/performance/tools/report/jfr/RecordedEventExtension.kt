package com.atlassian.performance.tools.report.jfr

import jdk.jfr.consumer.RecordedEvent
import jdk.jfr.consumer.RecordedThread

fun RecordedEvent.javaThreadId(): Long? {
    return if (this.hasField("sampledThread")) {
        this.getValue<RecordedThread>("sampledThread")?.javaThreadId
//        this.getValue<RecordedThread>("sampledThread")?.osThreadId
    } else {
        null
    }
}
