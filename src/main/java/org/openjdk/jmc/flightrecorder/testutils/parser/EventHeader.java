package org.openjdk.jmc.flightrecorder.testutils.parser;

import com.atlassian.performance.tools.report.jfr.VarInt;

import java.io.OutputStream;

public class EventHeader {
    /**
     * Size of the event in bytes, excluding the event "header" (eventSize and eventTypeId)
     */
    public final int size;
    public final long eventTypeId;

    public EventHeader(int size, long eventTypeId) {
        this.size = size;
        this.eventTypeId = eventTypeId;
    }

    public void write(OutputStream out) {
        VarInt.write(size, out);
        VarInt.write(eventTypeId, out);
    }

}
