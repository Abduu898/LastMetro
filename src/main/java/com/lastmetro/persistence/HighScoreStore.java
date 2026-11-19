package com.lastmetro.persistence;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Loads and safely updates the player's local best score. */
public final class HighScoreStore {
    private final Path scoreFile;

    public HighScoreStore(Path scoreFile) {
        this.scoreFile = scoreFile.toAbsolutePath();
    }

    public int load() {
        try {
            if (!Files.exists(scoreFile)) return 0;
            return Math.max(0, Integer.parseInt(Files.readString(scoreFile).trim()));
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    /** Saves only a new record and returns the record value that should be displayed. */
    public int saveIfHigher(int score) {
        int currentBest = load();
        int newBest = Math.max(currentBest, Math.max(0, score));
        if (newBest == currentBest) return currentBest;

        Path parent = scoreFile.getParent();
        Path temporaryFile = null;
        try {
            if (parent != null) Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(parent, scoreFile.getFileName().toString(), ".tmp");
            Files.writeString(temporaryFile, Integer.toString(newBest));
            try {
                Files.move(temporaryFile, scoreFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, scoreFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return newBest;
        } catch (IOException e) {
            return currentBest;
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // A leftover temporary file does not affect the saved record.
                }
            }
        }
    }
}
