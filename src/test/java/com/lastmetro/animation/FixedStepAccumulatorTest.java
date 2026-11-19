package com.lastmetro.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixedStepAccumulatorTest {
    @Test
    void combinesSmallFramesIntoConsistentTicks() {
        FixedStepAccumulator clock = new FixedStepAccumulator();

        assertEquals(0, clock.advance(0.01));
        assertEquals(1, clock.advance(0.01));
        assertTrue(clock.getInterpolationAlpha() >= 0.0);
        assertTrue(clock.getInterpolationAlpha() < 1.0);
    }

    @Test
    void capsCatchUpWorkAfterALargeFrame() {
        FixedStepAccumulator clock = new FixedStepAccumulator();

        assertEquals(5, clock.advance(2.0));
        assertTrue(clock.getInterpolationAlpha() < 1.0);
        assertEquals(0, clock.advance(0.0));
    }

    @Test
    void rejectsInvalidFrameTimes() {
        FixedStepAccumulator clock = new FixedStepAccumulator();
        assertThrows(IllegalArgumentException.class, () -> clock.advance(-0.1));
        assertThrows(IllegalArgumentException.class, () -> clock.advance(Double.NaN));
    }
}
