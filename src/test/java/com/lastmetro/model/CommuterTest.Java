package com.lastmetro.model;

import com.lastmetro.patterns.state.RunningState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommuterTest {
    private static final double GROUND_Y = 580.0;
    private Commuter player;

    @BeforeEach
    void setUp() {
        player = new Commuter(150.0, GROUND_Y);
    }

    @Test
    void constructorAndDirectStateAccessors() {
        assertEquals(150.0, player.getX());
        assertEquals(GROUND_Y - Commuter.STANDING_HEIGHT, player.getY());
        assertEquals(50.0, player.getWidth());
        assertEquals(Commuter.STANDING_HEIGHT, player.getHeight());
        assertEquals("RUNNING", player.getCurrentState().getStateName());
        assertFalse(player.isSlideHeld());
        assertFalse(player.isConfused());
        assertFalse(player.isSlowed());

        player.setVelocityY(12.5);
        assertEquals(12.5, player.getVelocityY());
        player.setState(new RunningState());
        assertEquals("RUNNING", player.getCurrentState().getStateName());
    }

    @Test
    void jumpReleaseAndGravityReturnToGround() {
        double initialY = player.getY();
        player.onJumpPressed();
        assertEquals("JUMPING", player.getCurrentState().getStateName());
        assertEquals(Commuter.JUMP_FORCE, player.getVelocityY());
        player.onJumpReleased();
        assertEquals(Commuter.JUMP_FORCE / 2.0, player.getVelocityY());

        player.update(0.1);
        assertTrue(player.getY() < initialY);
        for (int i = 0; i < 15; i++) {
            player.update(0.1);
        }
        assertEquals(GROUND_Y - Commuter.STANDING_HEIGHT, player.getY(), 0.01);
        assertEquals("RUNNING", player.getCurrentState().getStateName());
        assertEquals(0.0, player.getVelocityY());
    }

    @Test
    void jumpTappedJustBeforeLandingIsBufferedAndStartsAShortHop() {
        player.onJumpPressed();
        for (int frame = 0; frame < 35; frame++) player.update(1.0 / 60.0);
        assertEquals("JUMPING", player.getCurrentState().getStateName());

        player.onJumpReleased();
        player.onJumpPressed();
        player.onJumpReleased();
        for (int frame = 0; frame < 6; frame++) player.update(1.0 / 60.0);

        assertEquals("JUMPING", player.getCurrentState().getStateName());
        assertTrue(player.getVelocityY() < 0.0);
        assertTrue(player.getY() <= GROUND_Y - Commuter.STANDING_HEIGHT);
    }

    @Test
    void repeatedKeyDownWhileJumpButtonIsHeldDoesNotQueueAnAutomaticSecondJump() {
        player.onJumpPressed();
        for (int frame = 0; frame < 35; frame++) player.update(1.0 / 60.0);

        player.onJumpPressed(); // Operating systems may repeat key-down events while held.
        for (int frame = 0; frame < 8; frame++) player.update(1.0 / 60.0);

        assertEquals("RUNNING", player.getCurrentState().getStateName());
    }

    @Test
    void releaseInputsClearsASlideAndCancelsAQueuedJump() {
        player.onSlideChanged(true);
        player.releaseInputs();
        assertEquals("RUNNING", player.getCurrentState().getStateName());
        assertFalse(player.isSlideHeld());

        player.onJumpPressed();
        for (int frame = 0; frame < 35; frame++) player.update(1.0 / 60.0);
        player.onJumpReleased();
        player.onJumpPressed();
        player.releaseInputs();
        for (int frame = 0; frame < 8; frame++) player.update(1.0 / 60.0);

        assertEquals("RUNNING", player.getCurrentState().getStateName());
        assertFalse(player.isSlideHeld());
    }

    @Test
    void slideAndJumpCancelSlide() {
        player.onSlideChanged(true);
        assertTrue(player.isSlideHeld());
        assertEquals(Commuter.SLIDING_HEIGHT, player.getHeight());
        assertEquals(GROUND_Y - Commuter.SLIDING_HEIGHT, player.getY());
        assertEquals("SLIDING", player.getCurrentState().getStateName());

        player.onJumpPressed();
        assertEquals("JUMPING", player.getCurrentState().getStateName());
        assertEquals(Commuter.STANDING_HEIGHT, player.getHeight());
        assertEquals(Commuter.JUMP_FORCE, player.getVelocityY());

        assertTrue(player.isSlideHeld());
        player.update(0.8);
        assertEquals(GROUND_Y - Commuter.SLIDING_HEIGHT, player.getY(), 0.01);
        assertEquals("SLIDING", player.getCurrentState().getStateName());
        player.onSlideChanged(false);
        assertFalse(player.isSlideHeld());
        assertEquals("RUNNING", player.getCurrentState().getStateName());
    }

    @Test
    void confusedControlsInvertJumpAndSlide() {
        player.applyStenchDebuff(1.0);
        player.applySlowDebuff(1.0);
        assertTrue(player.isConfused());
        assertTrue(player.isSlowed());

        player.onSlideChanged(true);
        assertEquals("JUMPING", player.getCurrentState().getStateName());
        player.update(0.1);
        assertTrue(player.getY() < GROUND_Y - Commuter.STANDING_HEIGHT);

        player.applyStenchDebuff(0);
        player.applySlowDebuff(0);
        assertFalse(player.isConfused());
        assertFalse(player.isSlowed());
        player.onJumpPressed();
        player.onJumpReleased();
        player.onSlideChanged(true);
        player.onSlideChanged(false);
        assertTrue(player.isSlideHeld() == false);
    }

    @Test
    void debuffsExpireAndGroundHelpersWork() {
        player.applyStenchDebuff(0.2);
        player.applySlowDebuff(0.2);
        player.update(0.3);
        assertFalse(player.isConfused());
        assertFalse(player.isSlowed());

        player.enterSlideHitbox();
        player.setY(100);
        assertFalse(player.isAtOrBelowGround());
        player.snapToGround();
        assertTrue(player.isAtOrBelowGround());
        player.exitSlideHitbox();
        assertEquals(Commuter.STANDING_HEIGHT, player.getHeight());
        assertEquals(GROUND_Y - Commuter.STANDING_HEIGHT, player.getY());
    }

    @Test
    void resetRestoresFreshRunStateFromJumpingAndSliding() {
        player.onJumpPressed();
        player.update(0.1);
        player.applyStenchDebuff(5.0);
        player.applySlowDebuff(5.0);
        player.reset();

        assertFreshRunState();

        player.onSlideChanged(true);
        player.reset();

        assertFreshRunState();
    }

    private void assertFreshRunState() {
        assertEquals(150.0, player.getX());
        assertEquals(GROUND_Y - Commuter.STANDING_HEIGHT, player.getY());
        assertEquals(Commuter.STANDING_HEIGHT, player.getHeight());
        assertEquals(0.0, player.getVelocityY());
        assertFalse(player.isSlideHeld());
        assertEquals("RUNNING", player.getCurrentState().getStateName());
        assertFalse(player.isConfused());
        assertFalse(player.isSlowed());
    }
}
