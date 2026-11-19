package com.lastmetro.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HighScoreStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsTheParentDirectoryAndStoresNewRecords() {
        HighScoreStore store = new HighScoreStore(temporaryDirectory.resolve("settings/score.dat"));

        assertEquals(0, store.load());
        assertEquals(850, store.saveIfHigher(850));
        assertEquals(850, store.load());
    }

    @Test
    void lowerScoresDoNotReplaceTheRecordAndNegativeScoresAreIgnored() {
        HighScoreStore store = new HighScoreStore(temporaryDirectory.resolve("score.dat"));
        store.saveIfHigher(500);

        assertEquals(500, store.saveIfHigher(400));
        assertEquals(500, store.saveIfHigher(-10));
        assertEquals(500, store.load());
    }

    @Test
    void corruptScoreFileFallsBackToZero() throws IOException {
        Path scoreFile = temporaryDirectory.resolve("score.dat");
        Files.writeString(scoreFile, "not a score");
        HighScoreStore store = new HighScoreStore(scoreFile);

        assertEquals(0, store.load());
        assertEquals(25, store.saveIfHigher(25));
        assertEquals(25, store.load());
    }
}
