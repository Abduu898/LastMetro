package com.lastmetro.model;

import com.lastmetro.patterns.state.PlayerState;
import com.lastmetro.patterns.state.RunningState;

/**
 * The Parisian commuter sprinting for the last metro.
 */
public class Commuter extends Entity {
    public static final double STANDING_HEIGHT = 80.0;
    public static final double SLIDING_HEIGHT = 40.0;
    public static final double JUMP_FORCE = -780.0;
    public static final double GRAVITY = 2200.0;
    public static final double FAST_FALL_SPEED = 1100.0;

    private final double groundY;
    private double velocityY = 0;
    private boolean slideHeld = false;
    private PlayerState currentState;

    // Debuff timers (in seconds)
    private double stenchTimer = 0.0; // Inverts Jump and Slide controls
    private double slowTimer = 0.0;   // Slows down street scroll speed

    public Commuter(double x, double groundY) {
        super(x, groundY - STANDING_HEIGHT, 50.0, STANDING_HEIGHT);
        this.groundY = groundY;
        this.currentState = new RunningState();
    }

    @Override
    public void update(double deltaTime) {
        if (stenchTimer > 0) stenchTimer = Math.max(0, stenchTimer - deltaTime);
        if (slowTimer > 0) slowTimer = Math.max(0, slowTimer - deltaTime);
        currentState.update(this, deltaTime);
    }

    public void onJumpPressed() {
        if (isConfused()) {
            onSlideRaw(true);
        } else {
            currentState.handleJumpPress(this);
        }
    }

    public void onJumpReleased() {
        if (isConfused()) {
            onSlideRaw(false);
        } else {
            currentState.handleJumpRelease(this);
        }
    }

    public void onSlideChanged(boolean held) {
        if (isConfused()) {
            if (held) currentState.handleJumpPress(this);
            else currentState.handleJumpRelease(this);
        } else {
            onSlideRaw(held);
        }
    }

    private void onSlideRaw(boolean held) {
        this.slideHeld = held;
        currentState.handleSlide(this, held);
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