package com.lastmetro.animation;

/** Converts variable display-frame time into bounded, consistent simulation ticks. */
public final class FixedStepAccumulator {
    public static final double STEP_SECONDS = 1.0 / 60.0;
    private static final double MAX_FRAME_SECONDS = 0.25;
    private static final int MAX_STEPS_PER_FRAME = 5;

    private double accumulatedSeconds;

    public int advance(double frameDeltaSeconds) {
        if (!Double.isFinite(frameDeltaSeconds) || frameDeltaSeconds < 0.0) {
            throw new IllegalArgumentException("frame delta must be finite and non-negative");
        }
        accumulatedSeconds += Math.min(frameDeltaSeconds, MAX_FRAME_SECONDS);
        int availableSteps = (int) (accumulatedSeconds / STEP_SECONDS);
        int steps = Math.min(availableSteps, MAX_STEPS_PER_FRAME);
        accumulatedSeconds -= steps * STEP_SECONDS;
        if (availableSteps > MAX_STEPS_PER_FRAME) {
            accumulatedSeconds %= STEP_SECONDS;
        }
        return steps;
    }

    public double getInterpolationAlpha() {
        return accumulatedSeconds / STEP_SECONDS;
    }

    public void reset() {
        accumulatedSeconds = 0.0;
    }
}
