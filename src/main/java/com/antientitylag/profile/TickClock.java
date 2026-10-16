package com.antientitylag.profile;

/**
 * Sampled per-entity tick timer fed by {@code EntityTickMixin}.
 *
 * <p>Only 1 in 64 entity ticks is timed, so steady-state cost is a counter
 * increment plus a predictable branch (~1ns). A nesting guard protects
 * against re-entrant {@code Entity#tick} calls (passengers/vehicles): nested
 * ticks are counted but never timed.
 *
 * <p>Main-thread only by design; the flag is volatile so the client tick
 * thread can arm/disarm safely.
 */
public final class TickClock {
    private static volatile boolean armed = true;
    private static int depth;
    private static boolean measuring;
    private static long startNanos;
    private static long total;
    private static long sampledNanos;
    private static long sampledTicks;

    private static final long SAMPLE_MASK = 63L;

    private TickClock() {
    }

    public static void setArmed(boolean value) {
        armed = value;
    }

    public static void begin() {
        depth++;
        if (!armed || depth != 1) {
            return;
        }
        long n = total++;
        if ((n & SAMPLE_MASK) == 0L) {
            startNanos = System.nanoTime();
            measuring = true;
        } else {
            measuring = false;
        }
    }

    public static void end() {
        boolean shouldRecord = armed && depth == 1 && measuring;
        long elapsed = shouldRecord ? System.nanoTime() - startNanos : 0L;
        depth = Math.max(0, depth - 1);
        if (shouldRecord) {
            sampledNanos += elapsed;
            sampledTicks++;
        }
    }

    /**
     * Mean sampled entity-tick time in nanoseconds, or -1 when no samples
     * were taken since the last drain. Drains the accumulators.
     */
    public static double drainMeanNanos() {
        if (sampledTicks == 0) {
            return -1.0;
        }
        double mean = (double) sampledNanos / (double) sampledTicks;
        sampledNanos = 0L;
        sampledTicks = 0L;
        return mean;
    }

    public static long totalTicks() {
        return total;
    }

    static void resetForTests() {
        depth = 0;
        measuring = false;
        total = 0;
        sampledNanos = 0;
        sampledTicks = 0;
        armed = true;
    }
}
