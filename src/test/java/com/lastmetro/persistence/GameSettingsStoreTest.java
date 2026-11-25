package com.lastmetro.persistence;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GameSettingsStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void missingFileUsesDefaultControls() {
        GameSettingsStore store = new GameSettingsStore(temporaryDirectory.resolve("settings.properties"));

        assertEquals(GameSettings.defaults(), store.load());
    }

    @Test
    void changedControlsSurviveSavingAndReloading() {
        Path settingsFile = temporaryDirectory.resolve("nested/settings.properties");
        GameSettingsStore store = new GameSettingsStore(settingsFile);
        GameSettings customized = new GameSettings(KeyCode.W, KeyCode.DOWN);

        assertTrue(store.save(customized));
        assertEquals(customized, new GameSettingsStore(settingsFile).load());
    }

    @Test void audioAndMotionPreferencesSurviveRebindingAndOldFiles() throws IOException {
        Path file = temporaryDirectory.resolve("settings.properties");
        var store = new GameSettingsStore(file);
        var preferences = GameSettings.defaults().withSoundEnabled(false).withReducedMotion(true).withJumpKey(KeyCode.W);
        assertTrue(store.save(preferences));
        assertEquals(preferences, store.load());
        Files.writeString(file, "jump=W\nslide=DOWN\n");
        assertTrue(store.load().soundEnabled());
        assertFalse(store.load().reducedMotion());
        assertThrows(IllegalArgumentException.class, () -> new GameSettings(KeyCode.F8, KeyCode.DOWN));
        assertThrows(IllegalArgumentException.class, () -> new GameSettings(KeyCode.SPACE, KeyCode.F9));
    }

    @Test
    void malformedOrUnsafeSettingsFallBackToDefaults() throws IOException {
        Path settingsFile = temporaryDirectory.resolve("settings.properties");
        Files.writeString(settingsFile, "jump=NOT_A_KEY\nslide=DOWN\n");

        assertEquals(GameSettings.defaults(), new GameSettingsStore(settingsFile).load());

        Files.writeString(settingsFile, "jump=ESCAPE\nslide=DOWN\n");
        assertEquals(GameSettings.defaults(), new GameSettingsStore(settingsFile).load());
    }

    @Test
    void gameplayKeysCannotConflictOrOverridePauseKeys() {
        assertThrows(IllegalArgumentException.class, () -> new GameSettings(KeyCode.W, KeyCode.W));
        assertThrows(IllegalArgumentException.class, () -> new GameSettings(KeyCode.ESCAPE, KeyCode.DOWN));
        assertThrows(IllegalArgumentException.class, () -> new GameSettings(KeyCode.W, KeyCode.P));
        assertThrows(IllegalArgumentException.class, () -> new GameSettings(KeyCode.ENTER, KeyCode.DOWN));
    }

    @Test
    void saveFailureDoesNotThrowOrLeaveASettingsFile() throws IOException {
        Path blockedParent = temporaryDirectory.resolve("file-instead-of-folder");
        Files.writeString(blockedParent, "not a folder");
        GameSettingsStore store = new GameSettingsStore(blockedParent.resolve("settings.properties"));

        assertFalse(store.save(GameSettings.defaults()));
        assertFalse(Files.exists(blockedParent.resolve("settings.properties")));
    }
}
