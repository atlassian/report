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

import com.atlassian.performance.tools.report.jfr.VarInt;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RecordingStream implements AutoCloseable {
    private final DataInputStream delegate;
    private long position = 0;

    private List<Consumer<DataOutputStream>> writeLog = new ArrayList<>();
    private boolean isRecordingWrites = false;

    RecordingStream(InputStream is) {
        BufferedInputStream bis = (is instanceof BufferedInputStream) ? (BufferedInputStream) is
                : new BufferedInputStream(is);
        delegate = new DataInputStream(bis);
    }

    long position() {
        return position;
    }

    void startRecordingWrites() {
        isRecordingWrites = true;
    }

    void stopRecordingWrites() {
        isRecordingWrites = false;
    }

    void write(DataOutputStream os) {
        for (Consumer<DataOutputStream> write : writeLog) {
            write.accept(os);
        }
        writeLog.clear();
    }

    public void read(byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int read = delegate.read(buffer, offset, length);
            if (read == -1) {
                throw new IOException("Unexpected EOF");
            }
            if (isRecordingWrites) {
                byte[] copy = new byte[read];
                System.arraycopy(buffer, offset, copy, 0, read);
                writeLog.add(os -> {
                    try {
                        os.write(copy);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            offset += read;
            length -= read;
            position += read;
        }
    }

    public byte read() throws IOException {
        position += 1;
        byte result = delegate.readByte();
        if (isRecordingWrites) {
            writeLog.add(os -> {
                try {
                    os.writeByte(result);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return result;
    }

    short readShort() throws IOException {
        position += 2;
        short result = delegate.readShort();
        if (isRecordingWrites) {
            writeLog.add(os -> {
                try {
                    os.writeShort(result);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return result;
    }

    public int readInt() throws IOException {
        position += 4;
        int result = delegate.readInt();
        if (isRecordingWrites) {
            writeLog.add(os -> {
                try {
                    os.writeInt(result);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return result;
    }

    long readLong() throws IOException {
        position += 8;
        long result = delegate.readLong();
        if (isRecordingWrites) {
            writeLog.add(os -> {
                try {
                    os.writeLong(result);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return result;
    }

    public void addVarintWrite(long value) {
        if (isRecordingWrites) {
            writeLog.add(os -> {
                VarInt.write(value, os);
            });
        }
    }

    long readVarint() throws IOException {
        long value = 0;
        int readValue = 0;
        int i = 0;
        do {
            readValue = delegate.read();
            if (isRecordingWrites) {
                int finalReadValue1 = readValue;
                writeLog.add(os -> {
                    try {
                        os.writeByte(finalReadValue1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            int finalReadValue = readValue;
            value |= (long) (readValue & 0x7F) << (7 * i);
            i++;
        } while ((readValue & 0x80) != 0
                // In fact a fully LEB128 encoded 64bit number could take up to 10 bytes
                // (in order to store 64 bit original value using 7bit slots we need at most 10 of them).
                // However, eg. JMC parser will stop at 9 bytes, assuming that the compressed number is
                // a Java unsigned long (therefore having only 63 bits and they all fit in 9 bytes).
                && i < 9);
        position += i;
//        addVarintWrite(value);
        return value;
    }

    public int available() throws IOException {
        return delegate.available();
    }

    void skip(long bytes) throws IOException {
        long toSkip = bytes;
        while (toSkip > 0) {
            toSkip -= delegate.skip(toSkip);
        }
        position += bytes;
    }

    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
