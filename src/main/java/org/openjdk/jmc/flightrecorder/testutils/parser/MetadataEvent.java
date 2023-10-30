/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.flightrecorder.testutils.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * JFR Chunk metadata
 * <p>
 * It contains the chunk specific type specifications
 */
public final class MetadataEvent {
    private static final byte[] COMMON_BUFFER = new byte[4096]; // reusable byte buffer

    public final int size;
    public final long startTime;
    public final long duration;
    public final long metadataId;
    /**
     * Based on [jdk.jfr.internal.MetadataReader]
     */
    private int gmtOffset = 1;

    private final Map<Long, String> eventTypeNameMapBacking = new HashMap<>(256);
    private final LongMapping<String> eventTypeMap;

    MetadataEvent(RecordingStream stream, int eventSize, long eventType) throws IOException {
        size = eventSize;
        if (eventType != 0) {
            throw new IOException("Unexpected event type: " + eventType + " (should be 0). Stream at position: " + stream.position());
        }
        startTime = stream.readVarint();
        duration = stream.readVarint();
        metadataId = stream.readVarint();
        readElements(stream, readStringTable(stream));
        eventTypeMap = eventTypeNameMapBacking::get;
    }

    private MetadataEvent(int size, long startTime, long duration, long metadataId, Map<Long, String> eventTypeNameMapBacking) {
        this.size = size;
        this.startTime = startTime;
        this.duration = duration;
        this.metadataId = metadataId;
        this.eventTypeNameMapBacking.putAll(eventTypeNameMapBacking);
        this.eventTypeMap = eventTypeNameMapBacking::get;
    }

    public int getGmtOffset() {
        return gmtOffset;
    }

    public static class Builder {
        private int size;
        private long startTime;
        private long duration;
        private long metadataId;
        private Map<Long, String> eventTypeNameMapBacking = new HashMap<>(256);

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder metadataId(long metadataId) {
            this.metadataId = metadataId;
            return this;
        }

        public Builder eventTypeNameMapBacking(Map<Long, String> eventTypeNameMapBacking) {
            this.eventTypeNameMapBacking.putAll(eventTypeNameMapBacking);
            return this;
        }

        public MetadataEvent build() {
            return new MetadataEvent(size, startTime, duration, metadataId, eventTypeNameMapBacking);
        }
    }

    public Map<Long, String> getEventTypeNameMapBacking() {
        return Collections.unmodifiableMap(eventTypeNameMapBacking);
    }

    /**
     * Lazily compute and return the mappings of event type ids to event type names
     *
     * @return mappings of event type ids to event type names
     */
    public LongMapping<String> getEventTypeNameMap() {
        return eventTypeMap;
    }

    private String[] readStringTable(RecordingStream stream) throws IOException {
        int stringCnt = (int) stream.readVarint();
        String[] stringConstants = new String[stringCnt];
        for (int stringIdx = 0; stringIdx < stringCnt; stringIdx++) {
            stringConstants[stringIdx] = readUTF8(stream);
        }
        return stringConstants;
    }

    private void readElements(RecordingStream stream, String[] stringConstants) throws IOException {
        // get the element name
        int stringPtr = (int) stream.readVarint();
        boolean isClassElement = "class".equals(stringConstants[stringPtr]);

        // process the attributes
        int attrCount = (int) stream.readVarint();
        String superType = null;
        String name = null;
        String id = null;
        for (int i = 0; i < attrCount; i++) {
            int keyPtr = (int) stream.readVarint();
            int valPtr = (int) stream.readVarint();
            // ignore anything but 'class' elements
            if ("gmtOffset".equals(stringConstants[keyPtr])) {
                gmtOffset = Integer.parseInt(stringConstants[valPtr]);
            }
            if (isClassElement) {
                if ("superType".equals(stringConstants[keyPtr])) {
                    superType = stringConstants[valPtr];
                } else if ("name".equals(stringConstants[keyPtr])) {
                    name = stringConstants[valPtr];
                } else if ("id".equals(stringConstants[keyPtr])) {
                    id = stringConstants[valPtr];
                }
            }
        }
        // only event types are currently collected
        if (name != null && id != null && "jdk.jfr.Event".equals(superType)) {
            eventTypeNameMapBacking.put(Long.parseLong(id), name);
        }
        // now inspect all the enclosed elements
        int elemCount = (int) stream.readVarint();
        for (int i = 0; i < elemCount; i++) {
            readElements(stream, stringConstants);
        }
    }

    private String readUTF8(RecordingStream stream) throws IOException {
        byte id = stream.read();
        if (id == 0) {
            return null;
        } else if (id == 1) {
            return "";
        } else if (id == 3) {
            int size = (int) stream.readVarint();
            byte[] content = size <= COMMON_BUFFER.length ? COMMON_BUFFER : new byte[size];
            stream.read(content, 0, size);
            return new String(content, 0, size, StandardCharsets.UTF_8);
        } else if (id == 4) {
            int size = (int) stream.readVarint();
            char[] chars = new char[size];
            for (int i = 0; i < size; i++) {
                chars[i] = (char) stream.readVarint();
            }
            return new String(chars);
        } else {
            throw new IOException("Unexpected string constant id: " + id + ". Stream at position " + stream.position() + ", " + this);
        }
    }

    @Override
    public String toString() {
        return "Metadata{" + "size=" + size + ", startTime=" + startTime + ", duration=" + duration + ", metadataId="
                + metadataId + '}' + eventTypeNameMapBacking;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetadataEvent that = (MetadataEvent) o;
        return size == that.size && startTime == that.startTime && duration == that.duration && metadataId == that.metadataId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, startTime, duration, metadataId);
    }
}
