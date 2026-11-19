package com.lastmetro.persistence;

import javafx.scene.input.KeyCode;

import java.util.Objects;

/** Player preferences that should survive closing the game. */
public record GameSettings(KeyCode jumpKey, KeyCode slideKey) {
    public GameSettings {
        Objects.requireNonNull(jumpKey, "jumpKey");
        Objects.requireNonNull(slideKey, "slideKey");
        if (jumpKey == slideKey) throw new IllegalArgumentException("Jump and slide keys must be different");
        if (jumpKey == KeyCode.ESCAPE || jumpKey == KeyCode.P || jumpKey == KeyCode.ENTER
                || slideKey == KeyCode.ESCAPE || slideKey == KeyCode.P || slideKey == KeyCode.ENTER) {
            throw new IllegalArgumentException("Pause keys cannot be assigned to gameplay actions");
        }
    }

    public static GameSettings defaults() {
        return new GameSettings(KeyCode.SPACE, KeyCode.DOWN);
    }

    public GameSettings withJumpKey(KeyCode key) {
        return new GameSettings(key, slideKey);
    }

    public GameSettings withSlideKey(KeyCode key) {
        return new GameSettings(jumpKey, key);
    }
}
