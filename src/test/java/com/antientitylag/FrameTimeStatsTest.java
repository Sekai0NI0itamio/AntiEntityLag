package com.antientitylag;

import com.antientitylag.profile.FrameTimeStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrameTimeStatsTest {
    @Test
    void emptyReturnsZero() {
        FrameTimeStats stats = new FrameTimeStats(8);
        assertEquals(0, stats.count());
        assertEquals(0.0, stats.averageMs());
        assertEquals(0.0, stats.percentileMs(95.0));
    }

    @Test
    void averageAndPercentiles() {
        FrameTimeStats stats = new FrameTimeStats(8);
        for (long ms : new long[]{10, 20, 30, 40, 50}) {
            stats.record(ms * 1_000_000L);
        }
        assertEquals(5, stats.count());
        assertEquals(30.0, stats.averageMs(), 1e-9);
        assertEquals(30.0, stats.percentileMs(50.0), 1e-9);
        assertEquals(50.0, stats.percentileMs(95.0), 1e-9);
        assertEquals(50.0, stats.percentileMs(99.0), 1e-9);
        assertEquals(10.0, stats.percentileMs(0.0), 1e-9);
    }

    @Test
    void ringKeepsNewestSamples() {
        FrameTimeStats stats = new FrameTimeStats(4);
        for (long ms : new long[]{10, 20, 30, 40, 50}) {
            stats.record(ms * 1_000_000L);
        }
        assertEquals(4, stats.count());
        // Oldest (10ms) evicted: average of 20..50.
        assertEquals(35.0, stats.averageMs(), 1e-9);
    }

    @Test
    void constructorRejectsNonPositiveCapacity() {
        try {
            new FrameTimeStats(0);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
