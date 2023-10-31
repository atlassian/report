package com.atlassian.performance.tools.report.jfr

import org.openjdk.jmc.flightrecorder.testutils.parser.EventHeader
import java.time.Instant

class Event(
    val start: Instant,
    val header: EventHeader,
    val threadId: Long?
)
