package com.lastmetro.animation;

import java.util.Objects;

/** A reusable fade-out / state-change / fade-in transition for game screens. */
public final class ScreenTransition {
    private final double halfDuration;
    private Runnable midpointAction;
    private double elapsed;
    private int phase;
    private double alpha;

    public ScreenTransition(double halfDuration) {
        if (!Double.isFinite(halfDuration) || halfDuration <= 0.0) {
            throw new IllegalArgumentException("halfDuration must be finite and positive");
        }
        this.halfDuration = halfDuration;
    }

    public boolean start(Runnable action) {
        if (isRunning()) return false;
        midpointAction = Objects.requireNonNull(action, "action");
        elapsed = 0.0;
        alpha = 0.0;
        phase = 1;
        return true;
    }

    public void update(double deltaTime) {
        if (!Double.isFinite(deltaTime) || deltaTime < 0.0) {
            throw new IllegalArgumentException("deltaTime must be finite and non-negative");
        }
        double remaining = deltaTime;
        while (phase != 0 && remaining > 0.0) {
            double consumed = Math.min(remaining, halfDuration - elapsed);
            elapsed += consumed;
            remaining -= consumed;

            if (elapsed >= halfDuration) {
                if (phase == 1) {
                    alpha = 1.0;
                    phase = 2;
                    elapsed = 0.0;
                    Runnable action = midpointAction;
                    midpointAction = null;
                    action.run();
                } else {
                    phase = 0;
                    elapsed = 0.0;
                    alpha = 0.0;
                }
            } else {
                alpha = phase == 1 ? elapsed / halfDuration : 1.0 - elapsed / halfDuration;
            }
        }
    }

    public boolean isRunning() { return phase != 0; }
    /** Discard a pending action when focus loss must immediately pause gameplay. */
    public void cancel() { phase = 0; elapsed = alpha = 0; midpointAction = null; }
    public double getAlpha() { return alpha; }
}
