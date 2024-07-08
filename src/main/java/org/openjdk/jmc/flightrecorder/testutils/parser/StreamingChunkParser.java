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

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.jetbrains.annotations.NotNull;
import tools.profiler.jfr.converter.CheckpointEvent;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Streaming, almost zero-allocation, JFR chunk parser implementation. <br>
 * This is an MVP of a chunk parser allowing to stream the JFR events efficiently. The parser
 * notifies its listeners as the data becomes available. Because of this it is possible for the
 * metadata events to come 'out-of-band' (although not very probable) and it is up to the caller to
 * deal with that eventuality. <br>
 * This class is not thread-safe and is intended to be used from a single thread only.
 */
public final class StreamingChunkParser {
    private final ChunkParserListener chunkListener;
    private final CheckpointEvent.Listener checkpointListener;
    private MetadataEvent metadata;

    public StreamingChunkParser(ChunkParserListener chunkListener) {
        this(chunkListener, symbolPayload -> {
        });
    }

    public StreamingChunkParser(
            ChunkParserListener chunkListener,
            CheckpointEvent.Listener checkpointListener
    ) {
        this.chunkListener = chunkListener;
        this.checkpointListener = checkpointListener;
    }

    /**
     * Parse the given JFR recording stream.<br>
     * The parser will process the recording stream and call the provided listener in this order:
     * <ol>
     * <li>listener.onRecordingStart()
     * <li>listener.onChunkStart()
     * <li>listener.onEvent() | listener.onMetadata()
     * <li>listener.onChunkEnd()
     * <li>listener.onRecordingEnd()
     * </ol>
     */
    public void parse(Path inputFile) throws IOException {
        try (RecordingStream stream = new RecordingStream(new BufferedInputStream(Files.newInputStream(inputFile.toFile().toPath())))) {
            parse(inputFile, stream);
        }
        metadata = null;
    }

    private void parse(Path inputFile, RecordingStream stream) throws IOException {
        if (stream.available() == 0) {
            return;
        }
        try (RecordingFile jdkRecording = new RecordingFile(inputFile)) {
            chunkListener.onRecordingStart();
            int chunkCounter = 1;
            while (stream.available() > 0) {

                long chunkStartPos = stream.position();
                ChunkHeader header = ChunkHeader.read(stream);
                chunkListener.onChunkStart(chunkCounter, header);
                long chunkEndPos = chunkStartPos + (int) header.size;

                while (stream.position() < chunkEndPos) {
                    long eventStartPos = stream.position();
                    int eventSize = stream.readVarint();
                    if (eventSize > 0) {
                        parseEvent(stream, eventSize, eventStartPos, jdkRecording);
                    } else {
                        throw new IllegalStateException("Unexpected event size: " + eventSize + " at position " + stream.position());
                    }
                }
                chunkListener.onChunkEnd(chunkCounter, false);
                chunkCounter++;
            }
        } finally {
            chunkListener.onRecordingEnd();
        }

    }

    private void parseEvent(RecordingStream stream, int eventSize, long eventStartPos, RecordingFile jdkRecording) throws IOException {
        long eventType = stream.readVarlong();
        EventHeader eventHeader = new EventHeader(eventSize, eventType);
        byte[] eventPayload = getBytes(stream, eventSize, eventStartPos);
        if (eventType == 0) {
            metadata = new MetadataEvent(new RecordingStream(new ByteArrayInputStream(eventPayload)), eventSize, eventType);
            chunkListener.onMetadata(eventHeader, eventPayload, metadata);
        } else if (eventType == 1) {
            CheckpointEvent checkpoint = new CheckpointEvent(eventPayload, eventType, metadata, checkpointListener);
            chunkListener.onCheckpoint(eventHeader, eventPayload, checkpoint);
        } else {
            RecordedEvent jdkEvent = jdkRecording.readEvent();
            chunkListener.onEvent(jdkEvent, eventHeader, eventPayload);
        }
        // always skip any unconsumed event data to get the stream into consistent state
        stream.skip(eventSize - (stream.position() - eventStartPos));
    }

    @NotNull
    private byte[] getBytes(RecordingStream stream, int eventSize, long eventStartPos) throws IOException {
        long currentPos = stream.position();
        int payloadSize = (int) (eventSize - (currentPos - eventStartPos));
        byte[] eventPayload = new byte[payloadSize];
        stream.read(eventPayload, 0, payloadSize);
        return eventPayload;
    }
}
