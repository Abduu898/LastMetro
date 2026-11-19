package com.lastmetro.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameViewportTest {
    @Test
    void scalesWideViewWithoutDistortingLogicalCoordinates() {
        GameViewport viewport = new GameViewport(1280, 720);
        viewport.resize(1920, 1080);

        assertEquals(1.5, viewport.getScale(), 0.0001);
        assertTrue(viewport.contains(960, 540));
        assertEquals(640, viewport.toLogicalX(960), 0.0001);
        assertEquals(360, viewport.toLogicalY(540), 0.0001);
    }

    @Test
    void letterboxesTallViewAndRejectsClicksInTheBars() {
        GameViewport viewport = new GameViewport(1280, 720);
        viewport.resize(1000, 1000);

        assertTrue(viewport.contains(500, 500));
        assertFalse(viewport.contains(500, 100));
        assertEquals(360, viewport.toLogicalY(500), 0.0001);
    }

    @Test
    void zeroSizedViewDoesNotProduceClickableArea() {
        GameViewport viewport = new GameViewport(1280, 720);
        viewport.resize(0, 0);

        assertEquals(0, viewport.getScale());
        assertFalse(viewport.contains(0, 0));
    }
}
