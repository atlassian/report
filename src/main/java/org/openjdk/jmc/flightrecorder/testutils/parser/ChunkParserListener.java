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

import java.nio.file.Path;

/**
 * A callback to be provided to {@linkplain StreamingChunkParser#parse(Path, ChunkParserListener)}
 */
public interface ChunkParserListener {
	/** Called when the recording starts to be processed */
	default void onRecordingStart() {
	}

	/**
	 * Called for each discovered chunk
	 *
	 * @param chunkIndex
	 *            the chunk index (1-based)
	 * @param header
	 *            the parsed chunk header
	 */
	default void onChunkStart(int chunkIndex, ChunkHeader header) {
	}

	/**
	 * Called for the chunk metadata event
	 *
	 * @param metadata
	 *            the chunk metadata event
	 */
	default void onMetadata(EventHeader eventHeader, byte[] metadataPayload, MetadataEvent metadata) {
	}

	/**
	 * Called for each parsed event. Does not include metadata and checkpoint events.
	 *
	 * @param eventPayload
	 *            payload without the event "header"
	 */
	default void onEvent(RecordedEvent event, EventHeader eventHeader, byte[] eventPayload) {
	}

	default void onCheckpoint(EventHeader eventHeader, byte[] eventPayload) {
	}

	/**
	 * Called when a chunk is fully processed or skipped
	 *
	 * @param chunkIndex
	 *            the chunk index (1-based)
	 * @param skipped
	 *            {@literal true} if the chunk was skipped
	 */
	default void onChunkEnd(int chunkIndex, boolean skipped) {
	}

	/** Called when the recording was fully processed */
	default void onRecordingEnd() {
	}


}
