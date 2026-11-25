package com.lastmetro.model;

import com.lastmetro.patterns.state.PlayerState;
import com.lastmetro.patterns.state.JumpingState;
import com.lastmetro.patterns.state.RunningState;

/**
 * The Parisian commuter sprinting for the last metro.
 */
public class Commuter extends Entity {
    public static final double STANDING_HEIGHT = 80.0;
    public static final double SLIDING_HEIGHT = 40.0;
    public static final double JUMP_FORCE = -735.0;
    public static final double GRAVITY = 1900.0;
    public static final double FALL_GRAVITY_MULTIPLIER = 1.35;
    public static final double FAST_FALL_SPEED = 1050.0;
    public static final double JUMP_BUFFER_SECONDS = 0.16;
    private static final double RELEASED_BUFFERED_JUMP_RATIO = 0.55;

    private final double groundY;
    private final double startX;
    private double velocityY = 0;
    private double jumpBufferTimer = 0.0;
    private boolean jumpButtonHeld = false;
    private boolean slideHeld = false;
    private boolean jumpKeyDown = false;
    private boolean slideKeyDown = false;
    private boolean jumpKeyWasInverted = false;
    private boolean slideKeyWasInverted = false;
    private int jumpActionHolds = 0;
    private int slideActionHolds = 0;
    private PlayerState currentState;

    // Debuff timers (in seconds)
    private double stenchTimer = 0.0; // Inverts Jump and Slide controls
    private double slowTimer = 0.0;   // Slows down street scroll speed

    public Commuter(double x, double groundY) {
        super(x, groundY - STANDING_HEIGHT, 50.0, STANDING_HEIGHT);
        this.startX = x;
        this.groundY = groundY;
        this.currentState = new RunningState();
    }

    public void reset() {
        this.x = startX;
        this.height = STANDING_HEIGHT;
        this.y = groundY - STANDING_HEIGHT;
        this.velocityY = 0.0;
        this.jumpBufferTimer = 0.0;
        this.jumpButtonHeld = false;
        this.slideHeld = false;
        this.jumpKeyDown = false;
        this.slideKeyDown = false;
        this.jumpKeyWasInverted = false;
        this.slideKeyWasInverted = false;
        this.jumpActionHolds = 0;
        this.slideActionHolds = 0;
        this.currentState = new RunningState();
        this.stenchTimer = 0.0;
        this.slowTimer = 0.0;
    }

    @Override
    public void update(double deltaTime) {
        if (stenchTimer > 0) stenchTimer = Math.max(0, stenchTimer - deltaTime);
        if (slowTimer > 0) slowTimer = Math.max(0, slowTimer - deltaTime);
        if (jumpBufferTimer > 0) jumpBufferTimer = Math.max(0, jumpBufferTimer - deltaTime);
        currentState.update(this, deltaTime);
    }

    public void onJumpPressed() {
        if (jumpKeyDown) return;
        jumpKeyDown = true;
        jumpKeyWasInverted = isConfused();
        if (jumpKeyWasInverted) pressSlideAction();
        else pressJumpAction();
    }

    public void onJumpReleased() {
        if (!jumpKeyDown) return;
        jumpKeyDown = false;
        if (jumpKeyWasInverted) releaseSlideAction();
        else releaseJumpAction();
        jumpKeyWasInverted = false;
    }

    public void onSlideChanged(boolean held) {
        if (held) {
            if (slideKeyDown) return;
            slideKeyDown = true;
            slideKeyWasInverted = isConfused();
            if (slideKeyWasInverted) pressJumpAction();
            else pressSlideAction();
        } else {
            if (!slideKeyDown) return;
            slideKeyDown = false;
            if (slideKeyWasInverted) releaseJumpAction();
            else releaseSlideAction();
            slideKeyWasInverted = false;
        }
    }

    private void pressJumpAction() {
        if (jumpActionHolds++ == 0) jumpPressed();
    }

    private void releaseJumpAction() {
        if (jumpActionHolds > 0 && --jumpActionHolds == 0) jumpReleased();
    }

    private void pressSlideAction() {
        if (slideActionHolds++ == 0) onSlideRaw(true);
    }

    private void releaseSlideAction() {
        if (slideActionHolds > 0 && --slideActionHolds == 0) onSlideRaw(false);
    }

    private void onSlideRaw(boolean held) {
        this.slideHeld = held;
        currentState.handleSlide(this, held);
    }

    private void jumpPressed() {
        if (jumpButtonHeld) return;
        boolean wasJumping = currentState.getStateName().equals("JUMPING");
        jumpButtonHeld = true;
        jumpBufferTimer = JUMP_BUFFER_SECONDS;
        currentState.handleJumpPress(this);
        if (!wasJumping && currentState.getStateName().equals("JUMPING")) jumpBufferTimer = 0.0;
    }

    private void jumpReleased() {
        jumpButtonHeld = false;
        currentState.handleJumpRelease(this);
    }

    /** Starts a buffered jump when the current arc lands. */
    public boolean launchBufferedJumpIfReady() {
        if (jumpBufferTimer <= 0.0) return false;
        jumpBufferTimer = 0.0;
        velocityY = JUMP_FORCE * (jumpButtonHeld ? 1.0 : RELEASED_BUFFERED_JUMP_RATIO);
        currentState = new JumpingState();
        return true;
    }

    /** Clears held inputs when pausing or when the game window loses focus. */
    public void releaseInputs() {
        if (jumpKeyDown) onJumpReleased();
        if (slideKeyDown) onSlideChanged(false);
        jumpBufferTimer = 0.0;
        jumpKeyDown = false;
        slideKeyDown = false;
        jumpKeyWasInverted = false;
        slideKeyWasInverted = false;
        jumpActionHolds = 0;
        slideActionHolds = 0;
        jumpButtonHeld = false;
        if (slideHeld) onSlideRaw(false);
        if (currentState.getStateName().equals("SLIDING")) {
            exitSlideHitbox();
            currentState = new RunningState();
        }
        slideHeld = false;
    }

    public void applyStenchDebuff(double duration) {
        this.stenchTimer = duration;
    }

    public void applySlowDebuff(double duration) {
        this.slowTimer = duration;
    }

    public boolean isConfused() { return stenchTimer > 0; }
    public boolean isSlowed() { return slowTimer > 0; }

    public void enterSlideHitbox() {
        this.height = SLIDING_HEIGHT;
        this.y = groundY - SLIDING_HEIGHT;
    }

    public void exitSlideHitbox() {
        this.height = STANDING_HEIGHT;
        this.y = groundY - STANDING_HEIGHT;
    }

    public void snapToGround() {
        this.y = groundY - this.height;
    }

    public boolean isAtOrBelowGround() {
        return (this.y + this.height) >= groundY;
    }

    public double getVelocityY() { return velocityY; }
    public void setVelocityY(double velocityY) { this.velocityY = velocityY; }
    public boolean isSlideHeld() { return slideHeld; }
    public PlayerState getCurrentState() { return currentState; }
    public void setState(PlayerState state) { this.currentState = state; }
}
