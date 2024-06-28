/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package tools.profiler.jfr.converter;

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

    private static final int CHUNK_HEADER_SIZE = 68;

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
    public final Map<Long, String> symbols = new HashMap<>();
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
//            case "jdk.types.ChunkHeader":
//                stream.skip(CHUNK_HEADER_SIZE + 3);
//                break;
//            case "java.lang.Thread":
//                readThreads(type.fields.size());
//                break;
//            case "java.lang.Class":
//                readClasses(type.fields.size());
//                break;
            case "java.lang.String":
                readStrings();
                break;
            case "jdk.types.Symbol":
                readSymbols();
                break;
//            case "jdk.types.Method":
//                readMethods();
//                break;
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


    //    private void readThreads(int fieldCount) {
//        int count = threads.preallocate(getVarint());
//        for (int i = 0; i < count; i++) {
//            long id = getVarlong();
//            String osName = getString();
//            int osThreadId = getVarint();
//            String javaName = getString();
//            long javaThreadId = getVarlong();
//            readFields(fieldCount - 4);
//            threads.put(id, javaName != null ? javaName : osName);
//        }
//    }
//
//    private void readClasses(int fieldCount) {
//        int count = classes.preallocate(getVarint());
//        for (int i = 0; i < count; i++) {
//            long id = getVarlong();
//            long loader = getVarlong();
//            long name = getVarlong();
//            long pkg = getVarlong();
//            int modifiers = getVarint();
//            readFields(fieldCount - 4);
//            classes.put(id, new ClassRef(name));
//        }
//    }
//
//    private void readMethods() {
//        int count = methods.preallocate(getVarint());
//        for (int i = 0; i < count; i++) {
//            long id = getVarlong();
//            long cls = getVarlong();
//            long name = getVarlong();
//            long sig = getVarlong();
//            int modifiers = getVarint();
//            int hidden = getVarint();
//            methods.put(id, new MethodRef(cls, name, sig));
//        }
//    }
//
    private void readStackTraces() throws IOException {
        int count = stream.readVarint();
        for (int i = 0; i < count; i++) {
            long id = stream.readVarlong();
            int truncated = stream.readVarint();
            Object stackTrace = readStackTrace();
        }
    }

    private Object readStackTrace() throws IOException {
        int depth = stream.readVarint();
        long[] methods = new long[depth];
        byte[] types = new byte[depth];
        int[] locations = new int[depth];
        for (int i = 0; i < depth; i++) {
            methods[i] = stream.readVarlong();
            int line = stream.readVarint();
            int bci = stream.readVarint();
            locations[i] = line << 16 | (bci & 0xffff);
            types[i] = stream.read();
        }
        return new Object(); // methods, types, locations
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
            long id = stream.readVarlong();
            byte encoding = stream.read();
            if (encoding != 3) { // TODO maybe just reuse [readVarstring]
                throw new IllegalArgumentException("Invalid symbol encoding " + encoding);
            }
            byte[] symbolPayload = stream.readVarbytes();
            updateSymbol(symbolPayload);
            symbols.put(id, new String(symbolPayload));
        }
    }

    private void updateSymbol(byte[] symbolPayload) {
        listener.onSymbol(symbolPayload);
        int symbolEndPosition = (int) stream.position();
        // rewind before the payload, but after the varint in [readVarbytes]
        int symbolPosition = symbolEndPosition - symbolPayload.length;
        payload.position(symbolPosition);
        payload.put(symbolPayload);
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

    private void readFields(int count) throws IOException {
        while (count-- > 0) {
            stream.readVarlong();
        }
    }

    @FunctionalInterface
    public interface Listener {

        void onSymbol(byte[] symbolPayload);
    }

}
