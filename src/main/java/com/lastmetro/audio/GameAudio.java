package com.lastmetro.audio;

import com.lastmetro.feedback.RunFeedback.Event;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import javax.sound.sampled.*;

/** Offline audio with all loading and mixer operations kept off the JavaFX thread. */
public final class GameAudio implements AutoCloseable {
    private static final String[] VOICES = {"station", "hurry", "arrived", "missed"};
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        var thread = new Thread(r, "lastmetro-audio"); thread.setDaemon(true); return thread;
    });
    private final Map<String, Clip> clips = new HashMap<>();
    private volatile boolean enabled, active, closed, available;

    public GameAudio(boolean enabled) {
        this.enabled = enabled;
        submit(() -> {
            for (String name : new String[] {"ambience", "step", "jump", "land", "slide", "coin",
                    "hit", "heal", "warning", "doors", "train", "win", "lose", "station", "hurry", "arrived", "missed"}) {
                Clip clip = null;
                try (var source = GameAudio.class.getResourceAsStream("/audio/" + name + ".wav")) {
                    if (source == null) continue;
                    try (var input = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(source))) {
                        clip = AudioSystem.getClip(); clip.open(input);
                        gain(clip, switch (name) {
                            case "ambience" -> -20;
                            case "step" -> -17;
                            case "coin", "land" -> -11;
                            case "train", "warning" -> -8;
                            default -> -4;
                        });
                        clips.put(name, clip); available = true;
                    }
                } catch (Exception ignored) {
                    if (clip != null) clip.close();
                    // Machines without an audio output remain fully playable.
                }
            }
            resumeAmbience();
        });
    }

    public boolean isAvailable() { return available; }
    public boolean isEnabled() { return enabled; }
    private void submit(Runnable action) {
        if (!closed) try { worker.execute(action); } catch (RejectedExecutionException ignored) { }
    }
    private static void gain(Clip clip, float value) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            var control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), value)));
        }
    }
    private void resumeAmbience() {
        var loop = clips.get("ambience");
        if (loop != null && enabled && active && !loop.isRunning()) loop.loop(Clip.LOOP_CONTINUOUSLY);
    }
    public void setEnabled(boolean value) { if (enabled != value) { enabled = value; refresh(); } }
    public void setActive(boolean value) { if (active != value) { active = value; refresh(); } }
    private void refresh() {
        submit(() -> {
            if (!enabled || !active) clips.values().forEach(Clip::stop);
            else resumeAmbience();
        });
    }
    public void startRun(boolean catchMode) {
        submit(() -> { clips.values().forEach(Clip::stop); resumeAmbience(); });
        if (catchMode) voice("station");
    }
    public void event(Event event) {
        switch (event) {
            case JUMP -> play("jump"); case LAND -> play("land"); case STEP -> play("step");
            case SLIDE -> play("slide"); case TICKET, COIN -> play("coin"); case HIT -> play("hit");
            case HEART -> play("heal");
            case WARNING -> play("warning"); case HURRY -> voice("hurry");
            case TRAIN_ARRIVE -> play("warning"); case TRAIN_DEPART -> play("train"); case DOORS -> play("doors");
            case WIN -> { play("win"); voice("arrived"); }
            case LOSE -> { play("lose"); voice("missed"); }
        }
    }
    private void play(String name) {
        if (!enabled || !active) return;
        submit(() -> {
            if (!enabled || !active) return;
            Clip clip = clips.get(name);
            if (clip != null) { clip.stop(); clip.setFramePosition(0); clip.start(); }
        });
    }
    private void voice(String name) {
        submit(() -> { for (String other : VOICES) { var clip = clips.get(other); if (clip != null) clip.stop(); } });
        play(name);
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        worker.execute(() -> { clips.values().forEach(Clip::close); clips.clear(); });
        worker.shutdown();
    }
}
