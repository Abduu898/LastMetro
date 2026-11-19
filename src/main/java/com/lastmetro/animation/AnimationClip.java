package com.lastmetro.animation;

/** Immutable timing and frame-layout information for a sprite-sheet animation. */
public record AnimationClip(int frameCount, double frameDuration, boolean looping) {
    public AnimationClip {
        if (frameCount < 1) throw new IllegalArgumentException("frameCount must be positive");
        if (!Double.isFinite(frameDuration) || frameDuration <= 0.0) {
            throw new IllegalArgumentException("frameDuration must be finite and positive");
        }
    }

    public double duration() {
        return frameCount * frameDuration;
    }
}
