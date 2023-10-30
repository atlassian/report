package com.atlassian.performance.tools.report.jfr

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.openjdk.jmc.flightrecorder.testutils.parser.ChunkHeader

class TimeConverterTest {

    @Test
    fun shouldConvertTimespan() {
        // given
        val timeConverter = TimeConverter.of(
            ChunkHeader.Builder()
                .frequency(20)
                .build()
        )
        // when
        val nanos= timeConverter.convertTimespan(100)
        // then
        assertThat(nanos).isEqualTo(5_000_000_000L)
    }

    @Test
    fun shouldConvertTimestamp() {
        // given
        val timeConverter = TimeConverter.of(
            ChunkHeader.Builder()
                .startTicks(18)
                .startNanos(5)
                .frequency(20)
                .build()
        )
        // when
        val nanos= timeConverter.convertTimestamp(35)
        // then
        assertThat(nanos).isEqualTo(850_000_005L)
    }
}
