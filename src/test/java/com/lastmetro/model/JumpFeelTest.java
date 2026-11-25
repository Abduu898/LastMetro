package com.lastmetro.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JumpFeelTest {
    private double[] arc(double step, boolean tap) {
        var player = new Commuter(150, 576); player.onJumpPressed();
        if (tap) player.onJumpReleased();
        double apex = 0, elapsed = 0;
        while (elapsed < 2) {
            player.update(step); elapsed += step;
            apex = Math.max(apex, 576 - player.getY() - player.getHeight());
            if (player.getCurrentState().getStateName().equals("RUNNING")) break;
        }
        assertEquals(576, player.getY() + player.getHeight(), .001);
        assertEquals(0, player.getVelocityY()); return new double[] {apex, elapsed};
    }
    @Test void fullJumpHasAReadableArcAndTapProducesAUsefulShortHop() {
        var full = arc(1.0 / 120, false); var tap = arc(1.0 / 120, true);
        assertTrue(full[0] > 135 && full[0] < 148);
        assertTrue(full[1] > .68 && full[1] < .78);
        assertTrue(tap[0] > 20 && tap[0] < 30);
        assertTrue(tap[1] < full[1] * .6);
    }
    @Test void arcIsConsistentAcrossCommonSimulationRates() {
        var sixty = arc(1.0 / 60, false); var oneTwenty = arc(1.0 / 120, false);
        assertEquals(oneTwenty[0], sixty[0], 1.5); assertEquals(oneTwenty[1], sixty[1], .025);
    }
    @Test void downFastFallsAndHeldSlideTransitionsCleanlyAtLanding() {
        var player = new Commuter(150, 576); player.onJumpPressed();
        for (int i = 0; i < 18; i++) player.update(1.0 / 60);
        player.onSlideChanged(true); assertEquals(Commuter.FAST_FALL_SPEED, player.getVelocityY());
        for (int i = 0; i < 18; i++) player.update(1.0 / 60);
        assertEquals("SLIDING", player.getCurrentState().getStateName());
        assertEquals(576, player.getY() + player.getHeight()); player.releaseInputs();
        assertEquals("RUNNING", player.getCurrentState().getStateName());
    }
}
