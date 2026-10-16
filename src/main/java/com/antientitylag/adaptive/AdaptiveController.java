package com.antientitylag.adaptive;

/**
 * Hysteresis-based adaptive quality controller.
 *
 * <p>Tracks an exponential moving average of frame time and moves through
 * levels 0 (full quality) to 3 (maximum savings). Level increases require
 * several consecutive over-threshold frames; decreases require a longer run of
 * calm frames plus a cooldown, so quality never oscillates with a single
 * spike. Decisions use the smoothed average only — never instantaneous FPS.
 */
public final class AdaptiveController {
    public static final int MAX_LEVEL = 3;

    private final double smoothing;
    private final double[] upThresholdMs;
    private final double[] downThresholdMs;
    private final int upFrames;
    private final int downFrames;
    private final int cooldownFrames;

    private double emaMs;
    private boolean primed;
    private int level;
    private int aboveCount;
    private int belowCount;
    private int cooldown;

    public AdaptiveController() {
        this(0.15,
                new double[]{19.5, 23.0, 28.0},
                new double[]{16.5, 19.0, 23.5},
                20, 120, 60);
    }

    public AdaptiveController(
            double smoothing,
            double[] upThresholdMs,
            double[] downThresholdMs,
            int upFrames,
            int downFrames,
            int cooldownFrames) {
        if (upThresholdMs.length != MAX_LEVEL || downThresholdMs.length != MAX_LEVEL) {
            throw new IllegalArgumentException("threshold arrays must have length " + MAX_LEVEL);
        }
        this.smoothing = smoothing;
        this.upThresholdMs = upThresholdMs.clone();
        this.downThresholdMs = downThresholdMs.clone();
        this.upFrames = upFrames;
        this.downFrames = downFrames;
        this.cooldownFrames = cooldownFrames;
    }

    public void onFrame(double frameMs) {
        if (!primed) {
            emaMs = frameMs;
            primed = true;
            return;
        }
        emaMs += smoothing * (frameMs - emaMs);
        if (cooldown > 0) {
            cooldown--;
            aboveCount = 0;
            belowCount = 0;
            return;
        }
        if (level < MAX_LEVEL && emaMs > upThresholdMs[level]) {
            aboveCount++;
            belowCount = 0;
            if (aboveCount >= upFrames) {
                level++;
                aboveCount = 0;
                cooldown = cooldownFrames;
            }
        } else if (level > 0 && emaMs < downThresholdMs[level - 1]) {
            belowCount++;
            aboveCount = 0;
            if (belowCount >= downFrames) {
                level--;
                belowCount = 0;
                cooldown = cooldownFrames;
            }
        } else {
            aboveCount = 0;
            belowCount = 0;
        }
    }

    public int level() {
        return level;
    }

    public double emaMs() {
        return emaMs;
    }

    public void reset() {
        primed = false;
        emaMs = 0.0;
        level = 0;
        aboveCount = 0;
        belowCount = 0;
        cooldown = 0;
    }
}
