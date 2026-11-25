package com.lastmetro.animation;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScreenTransitionTest {
    @Test void cancellingFocusTransitionDiscardsItsPendingAction() {
        var transition = new ScreenTransition(.2);
        var changes = new AtomicInteger();
        transition.start(changes::incrementAndGet); transition.update(.1);
        transition.cancel(); transition.update(1);
        assertFalse(transition.isRunning()); assertEquals(0, transition.getAlpha()); assertEquals(0, changes.get());
    }
    @Test
    void fadesOutRunsScreenChangeThenFadesBackIn() {
        ScreenTransition transition = new ScreenTransition(0.2);
        AtomicInteger changes = new AtomicInteger();
        assertTrue(transition.start(changes::incrementAndGet));

        transition.update(0.1);
        assertEquals(0.5, transition.getAlpha(), 0.001);
        assertEquals(0, changes.get());

        transition.update(0.1);
        assertEquals(1.0, transition.getAlpha(), 0.001);
        assertEquals(1, changes.get());

        transition.update(0.1);
        assertEquals(0.5, transition.getAlpha(), 0.001);
        transition.update(0.1);
        assertEquals(0.0, transition.getAlpha(), 0.001);
        assertFalse(transition.isRunning());
        assertEquals(1, changes.get());
    }

    @Test
    void doesNotStartAnOverlappingTransition() {
        ScreenTransition transition = new ScreenTransition(0.2);
        AtomicInteger changes = new AtomicInteger();
        assertTrue(transition.start(changes::incrementAndGet));
        assertFalse(transition.start(changes::incrementAndGet));

        transition.update(0.4);

        assertEquals(1, changes.get());
        assertFalse(transition.isRunning());
    }
}
