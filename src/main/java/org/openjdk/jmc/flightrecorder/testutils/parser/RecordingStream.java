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

import java.io.*;

public final class RecordingStream implements AutoCloseable {
    private final DataInputStream delegate;
    private long position = 0;
    private final ByteArrayOutputStream toBytesLog = new ByteArrayOutputStream();
    private final DataOutputStream toBytesStream = new DataOutputStream(toBytesLog);
    private boolean isRecordingWrites = false;

    public RecordingStream(InputStream is) {
        BufferedInputStream bis = (is instanceof BufferedInputStream) ? (BufferedInputStream) is
                : new BufferedInputStream(is);
        delegate = new DataInputStream(bis);
    }

    public long position() {
        return position;
    }

    void startRecordingWrites() {
        isRecordingWrites = true;
    }

    byte[] stopRecordingWrites() {
        isRecordingWrites = false;
        byte[] result = toBytesLog.toByteArray();
        toBytesLog.reset();
        return result;
    }

    public void read(byte[] buffer, int offset, int length) throws IOException {
        int read = delegate.read(buffer, offset, length);
        if (read == -1) {
            throw new IOException("Unexpected EOF");
        }
        position += read;
        if (isRecordingWrites) {
            toBytesStream.write(buffer, offset, read);
        }
    }

    public byte read() throws IOException {
        position += 1;
        byte result = delegate.readByte();
        if (isRecordingWrites) {
            toBytesStream.writeByte(result);
        }
        return result;
    }

    short readShort() throws IOException {
        position += 2;
        short result = delegate.readShort();
        if (isRecordingWrites) {
            toBytesStream.writeShort(result);
        }
        return result;
    }

    public int readInt() throws IOException {
        position += 4;
        int result = delegate.readInt();
        if (isRecordingWrites) {
            toBytesStream.writeInt(result);
        }
        return result;
    }

    long readLong() throws IOException {
        position += 8;
        long result = delegate.readLong();
        if (isRecordingWrites) {
            toBytesStream.writeLong(result);
        }
        return result;
    }

    public long readVarlong() throws IOException {
        long value = 0;
        int readValue = 0;
        int i = 0;
        do {
            readValue = read();
            value |= (long) (readValue & 0x7F) << (7 * i);
            i++;
        } while ((readValue & 0x80) != 0
                // In fact a fully LEB128 encoded 64bit number could take up to 10 bytes
                // (in order to store 64 bit original value using 7bit slots we need at most 10 of them).
                // However, eg. JMC parser will stop at 9 bytes, assuming that the compressed number is
                // a Java unsigned long (therefore having only 63 bits and they all fit in 9 bytes).
                && i < 9);
        return value;
    }

    public int readVarint() throws IOException {
        return (int) readVarlong();
    }

    public byte[] readVarbytes() throws IOException {
        int byteCount = readVarint();
        byte[] bytes = new byte[byteCount];
        read(bytes, 0, byteCount);
        return bytes;
    }

    public int available() throws IOException {
        return delegate.available();
    }

    public void skip(long bytes) throws IOException {
        position += delegate.skip(bytes);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
