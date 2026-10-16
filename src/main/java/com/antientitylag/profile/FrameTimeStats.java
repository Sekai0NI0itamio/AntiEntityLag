package com.antientitylag.profile;

import java.util.Arrays;

/**
 * Fixed-capacity ring buffer of frame times in nanoseconds.
 *
 * <p>Recording is allocation-free (plain array store). Query methods
 * ({@link #percentileMs}) allocate a scratch copy, so they must only run on
 * diagnostic paths (overlay refresh, profile export), never in hot loops.
 */
public final class FrameTimeStats {
    private final long[] buf;
    private int head;
    private int count;

    public FrameTimeStats(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.buf = new long[capacity];
    }

    public void record(long nanos) {
        buf[head] = nanos;
        head = (head + 1) % buf.length;
        if (count < buf.length) {
            count++;
        }
    }

    public int count() {
        return count;
    }

    public int capacity() {
        return buf.length;
    }

    public void reset() {
        head = 0;
        count = 0;
    }

    public double averageMs() {
        if (count == 0) {
            return 0.0;
        }
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sum += buf[i];
        }
        return sum / (double) count / 1_000_000.0;
    }

    /**
     * Nearest-rank percentile in milliseconds. p is 0-100 inclusive.
     */
    public double percentileMs(double p) {
        if (count == 0) {
            return 0.0;
        }
        double clamped = Math.min(100.0, Math.max(0.0, p));
        long[] copy = new long[count];
        System.arraycopy(buf, 0, copy, 0, count);
        Arrays.sort(copy);
        int rank = (int) Math.ceil(clamped / 100.0 * count) - 1;
        rank = Math.min(count - 1, Math.max(0, rank));
        return copy[rank] / 1_000_000.0;
    }
}
