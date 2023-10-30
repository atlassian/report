package org.openjdk.jmc.flightrecorder.testutils.parser;

public class EventHeader {
    /**
     * Size of the event in bytes, excluding the event "header" (eventSize and eventTypeId)
     */
    public final long size;
    public final long eventTypeId;
    /**
     * Exact bytes from which size and eventTypeId were read (in this order)
     */
    public final byte[] bytes;

    public EventHeader(long size, long eventTypeId, byte[] bytes) {
        this.size = size;
        this.eventTypeId = eventTypeId;
        this.bytes = bytes;
    }

    /**
     * @return total event size, including header and payload
     */
    public long eventSize() {
        return bytes.length + size;
    }
}
