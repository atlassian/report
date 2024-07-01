/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package tools.profiler.jfr.converter;

import com.atlassian.performance.tools.report.api.jfr.MutableJvmSymbol;
import org.openjdk.jmc.flightrecorder.testutils.parser.MetadataEvent;
import org.openjdk.jmc.flightrecorder.testutils.parser.RecordingStream;
import org.openjdk.jmc.flightrecorder.testutils.parser.StreamingChunkParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The checkpoint (AKA constant pool) event.
 * It was not parsed in the original [{@link StreamingChunkParser}.
 * This is an attempt to parse it, at least to cover the `jdk.types.Symbol` pool.
 * Most of the impl copied from [one.jfr.JfrReader].
 */
public final class CheckpointEvent {

    private final Listener listener;

    private final RecordingStream stream;
    private final MetadataEvent metadata;
    private final ByteBuffer payload;

    public final int size;
    public final long startTime;
    public final long duration;
    public final long delta;
    public final byte mask;
    public final int poolCount;

    public final Map<Long, String> strings = new HashMap<>();
    public final Map<String, Map<Integer, String>> enums = new HashMap<>();

    public CheckpointEvent(byte[] eventPayload, long eventType, MetadataEvent metadata, Listener listener) throws IOException {
        if (eventType != 1) {
            throw new IOException("Unexpected event type: " + eventType + " (should be 1)");
        }
        this.payload = ByteBuffer.wrap(eventPayload.clone());
        this.stream = new RecordingStream(new ByteArrayInputStream(eventPayload));
        this.metadata = metadata;
        this.listener = listener;
        size = eventPayload.length;
        startTime = stream.readVarlong();
        duration = stream.readVarlong();
        delta = stream.readVarlong();
        mask = stream.read();
        poolCount = stream.readVarint();
        for (int i = 0; i < poolCount; i++) {
            int typeId = stream.readVarint();
            JfrClass type = metadata.type(typeId);
            readConstants(type);
        }
    }

    public byte[] payload() {
        return payload.array(); // no defensive copy for now I guess
    }

    private void readConstants(JfrClass type) throws IOException {
        switch (type.name) {
            case "java.lang.String":
                readStrings();
                break;
            case "jdk.types.Symbol":
                readSymbols();
                break;
            case "jdk.types.StackTrace":
                readStackTraces();
                break;
            default:
                if (type.simpleType && type.fields.size() == 1) {
                    readEnumValues(type.name);
                } else {
                    readOtherConstants(type.fields);
                }
        }
    }

    private void readStackTraces() throws IOException {
        int count = stream.readVarint();
        for (int i = 0; i < count; i++) {
            stream.readVarlong();
            stream.readVarint();
            readStackTrace();
        }
    }

    private void readStackTrace() throws IOException {
        int depth = stream.readVarint();
        for (int i = 0; i < depth; i++) {
            stream.readVarlong();
            stream.readVarint();
            stream.readVarint();
            stream.read();
        }
    }

    private void readStrings() throws IOException {
        int count = stream.readVarint();
        for (int i = 0; i < count; i++) {
            strings.put(stream.readVarlong(), readVarstring());
        }
    }

    public String readVarstring() throws IOException {
        byte encoding = stream.read();
        switch (encoding) {
            case 0:
                return null;
            case 1:
                return "";
            case 2:
                return strings.get(stream.readVarlong());
            case 3:
                return new String(stream.readVarbytes(), StandardCharsets.UTF_8);
            case 4: {
                char[] chars = new char[stream.readVarint()];
                for (int i = 0; i < chars.length; i++) {
                    chars[i] = (char) stream.readVarint();
                }
                return new String(chars);
            }
            case 5:
                return new String(stream.readVarbytes(), StandardCharsets.ISO_8859_1);
            default:
                throw new IllegalArgumentException("Invalid string encoding");
        }
    }

    private void readSymbols() throws IOException {
        int count = stream.readVarint();
        for (int i = 0; i < count; i++) {
            stream.readVarlong();
            byte encoding = stream.read();
            if (encoding != 3) {
                throw new IllegalArgumentException("Invalid symbol encoding " + encoding);
            }
            MutableJvmSymbol symbolPayload = new MutableJvmSymbol(stream.readVarbytes());
            updateSymbol(symbolPayload);
        }
    }

    private void updateSymbol(MutableJvmSymbol symbol) {
        listener.onSymbol(symbol);
        int symbolEndPosition = (int) stream.position();
        // rewind before the payload, but after the varint in [readVarbytes]
        int symbolPosition = symbolEndPosition - symbol.getPayload().length;
        payload.position(symbolPosition);
        payload.put(symbol.getPayload());
    }

    private void readEnumValues(String typeName) throws IOException {
        HashMap<Integer, String> map = new HashMap<>();
        int count = stream.readVarint();
        for (int i = 0; i < count; i++) {
            map.put((int) stream.readVarlong(), readVarstring());
        }
        enums.put(typeName, map);
    }

    private void readOtherConstants(List<JfrField> fields) throws IOException {
        int stringType = getTypeId("java.lang.String");

        boolean[] numeric = new boolean[fields.size()];
        for (int i = 0; i < numeric.length; i++) {
            JfrField f = fields.get(i);
            numeric[i] = f.constantPool || f.type != stringType;
        }

        int count = stream.readVarint();
        for (int i = 0; i < count; i++) {
            stream.readVarlong();
            readFields(numeric);
        }
    }

    private int getTypeId(String typeName) {
        JfrClass type = metadata.typesByName.get(typeName);
        return type != null ? type.id : -1;
    }

    private void readFields(boolean[] numeric) throws IOException {
        for (boolean n : numeric) {
            if (n) {
                stream.readVarlong();
            } else {
                readVarstring();
            }
        }
    }

    @FunctionalInterface
    public interface Listener {

        void onSymbol(MutableJvmSymbol symbolPayload);
    }

}
