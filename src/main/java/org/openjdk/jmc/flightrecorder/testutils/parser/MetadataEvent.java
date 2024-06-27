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

import tools.profiler.jfr.converter.Element;
import tools.profiler.jfr.converter.JfrClass;
import tools.profiler.jfr.converter.JfrField;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.Integer.parseInt;

/**
 * JFR Chunk metadata
 * <p>
 * It contains the chunk specific type specifications
 */
public final class MetadataEvent {

    // TODO WTF! this is not even that MetadataEvent instances are not thread-safe, the whole class itself is not thread-safe
    private static final byte[] COMMON_BUFFER = new byte[4096]; // reusable byte buffer

    public final int size;
    public final long startTime;
    public final long duration;
    public final long metadataId;
    /**
     * Based on [jdk.jfr.internal.MetadataReader]
     */
    private int gmtOffset = 1;

    public final Map<Integer, JfrClass> typesById = new HashMap<>(256);
    public final Map<String, JfrClass> typesByName = new HashMap<>(256);

    MetadataEvent(RecordingStream stream, int eventSize, long eventType) throws IOException {
        size = eventSize;
        if (eventType != 0) {
            throw new IOException("Unexpected event type: " + eventType + " (should be 0). Stream at position: " + stream.position());
        }
        startTime = stream.readVarlong();
        duration = stream.readVarlong();
        metadataId = stream.readVarlong();
        readElement(stream, readStringTable(stream));
    }

    private MetadataEvent(int size, long startTime, long duration, long metadataId) {
        this.size = size;
        this.startTime = startTime;
        this.duration = duration;
        this.metadataId = metadataId;
    }

    public int getGmtOffset() {
        return gmtOffset;
    }

    public JfrClass type(int id) {
        return typesById.get(id);
    }

    public static class Builder {
        private int size;
        private long startTime;
        private long duration;
        private long metadataId;

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

        public MetadataEvent build() {
            return new MetadataEvent(size, startTime, duration, metadataId);
        }
    }

    private String[] readStringTable(RecordingStream stream) throws IOException {
        int stringCnt = stream.readVarint();
        String[] stringConstants = new String[stringCnt];
        for (int stringIdx = 0; stringIdx < stringCnt; stringIdx++) {
            stringConstants[stringIdx] = readUTF8(stream);
        }
        return stringConstants;
    }

    private Element readElement(RecordingStream stream, String[] strings) throws IOException {
        String name = strings[stream.readVarint()];

        int attributeCount = stream.readVarint();
        Map<String, String> attributes = new HashMap<>(attributeCount);
        for (int i = 0; i < attributeCount; i++) {
            attributes.put(strings[stream.readVarint()], strings[stream.readVarint()]);
        }

        if (attributes.containsKey("gmtOffset")) {
            gmtOffset = parseInt(attributes.get("gmtOffset"));
        }

        Element e = createElement(name, attributes);
        int childCount = stream.readVarint();
        for (int i = 0; i < childCount; i++) {
            e.addChild(readElement(stream, strings));
        }
        return e;
    }

    private Element createElement(String name, Map<String, String> attributes) {
        switch (name) {
            case "class": {
                JfrClass type = new JfrClass(attributes);
                if (!attributes.containsKey("superType")) {
                    typesById.put(type.id, type);
                }
                typesByName.put(type.name, type);
                return type;
            }
            case "field":
                return new JfrField(attributes);
            default:
                return new Element();
        }
    }

    private String readUTF8(RecordingStream stream) throws IOException {
        byte id = stream.read();
        if (id == 0) {
            return null;
        } else if (id == 1) {
            return "";
        } else if (id == 3) {
            int size = stream.readVarint();
            byte[] content = size <= COMMON_BUFFER.length ? COMMON_BUFFER : new byte[size];
            stream.read(content, 0, size);
            return new String(content, 0, size, StandardCharsets.UTF_8);
        } else if (id == 4) {
            int size = stream.readVarint();
            char[] chars = new char[size];
            for (int i = 0; i < size; i++) {
                chars[i] = (char) stream.readVarlong();
            }
            return new String(chars);
        } else {
            throw new IOException("Unexpected string constant id: " + id + ". Stream at position " + stream.position() + ", " + this);
        }
    }

    @Override
    public String toString() {
        return "Metadata{" + "size=" + size + ", startTime=" + startTime + ", duration=" + duration + ", metadataId="
                + metadataId + '}';
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
