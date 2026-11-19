package com.lastmetro.persistence;

import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Small, versionable preferences store; bad or old files safely fall back to defaults. */
public final class GameSettingsStore {
    private final Path file;

    public GameSettingsStore(Path file) {
        this.file = file.toAbsolutePath();
    }

    public GameSettings load() {
        if (!Files.isRegularFile(file)) return GameSettings.defaults();
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            KeyCode jump = KeyCode.valueOf(properties.getProperty("jump", "SPACE"));
            KeyCode slide = KeyCode.valueOf(properties.getProperty("slide", "DOWN"));
            return new GameSettings(jump, slide);
        } catch (IOException | IllegalArgumentException exception) {
            return GameSettings.defaults();
        }
    }

    public boolean save(GameSettings settings) {
        Path parent = file.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, "lastmetro-settings-", ".tmp");
            Properties properties = new Properties();
            properties.setProperty("jump", settings.jumpKey().name());
            properties.setProperty("slide", settings.slideKey().name());
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Last Metro player settings");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            return false;
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }
}
