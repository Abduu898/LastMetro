package com.lastmetro.animation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Advances named sprite-sheet clips independently of the renderer. */
public final class AnimationPlayer {
    private final Map<String, AnimationClip> clips = new LinkedHashMap<>();
    private String currentName;
    private double elapsed;

    public void add(String name, AnimationClip clip) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Animation name is required");
        if (clips.putIfAbsent(name, Objects.requireNonNull(clip, "clip")) != null) {
            throw new IllegalArgumentException("Animation already exists: " + name);
        }
        if (currentName == null) currentName = name;
    }

    public void play(String name) {
        if (!clips.containsKey(name)) throw new IllegalArgumentException("Unknown animation: " + name);
        if (!name.equals(currentName)) {
            currentName = name;
            elapsed = 0.0;
        }
    }

    public void restart() {
        elapsed = 0.0;
    }

    public void update(double deltaTime) {
        if (!Double.isFinite(deltaTime) || deltaTime < 0.0) {
            throw new IllegalArgumentException("deltaTime must be finite and non-negative");
        }
        if (currentName == null || deltaTime == 0.0) return;

        AnimationClip clip = clips.get(currentName);
        if (clip.looping()) {
            elapsed = (elapsed + deltaTime) % clip.duration();
        } else {
            elapsed = Math.min(clip.duration(), elapsed + deltaTime);
        }
    }

    public int getCurrentFrame() {
        if (currentName == null) return 0;
        AnimationClip clip = clips.get(currentName);
        return Math.min(clip.frameCount() - 1, (int) (elapsed / clip.frameDuration()));
    }

    public String getCurrentName() { return currentName; }

    public boolean isFinished() {
        return currentName != null && !clips.get(currentName).looping()
                && elapsed >= clips.get(currentName).duration();
    }
}
