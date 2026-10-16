package com.antientitylag;

import com.antientitylag.profile.TickClock;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TickClockTest {
    @Test
    void samplesWithoutCrashingAndCountsTicks() throws Exception {
        reset();
        long before = TickClock.totalTicks();
        for (int i = 0; i < 256; i++) {
            TickClock.begin();
            TickClock.end();
        }
        assertTrue(TickClock.totalTicks() - before == 256);
        // 256 ticks at 1-in-64 sampling must produce samples.
        assertTrue(TickClock.drainMeanNanos() >= 0.0);
        // Drained: second drain reports no samples.
        assertTrue(TickClock.drainMeanNanos() < 0.0);
    }

    @Test
    void nestedTicksDoNotCorrupt() throws Exception {
        reset();
        TickClock.begin();
        TickClock.begin();
        TickClock.end();
        TickClock.end();
        assertTrue(TickClock.drainMeanNanos() >= 0.0);
    }

    private static void reset() throws Exception {
        Method reset = TickClock.class.getDeclaredMethod("resetForTests");
        reset.setAccessible(true);
        reset.invoke(null);
    }
}
