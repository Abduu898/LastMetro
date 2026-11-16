package com.lastmetro.model;

import com.lastmetro.patterns.PlayerState;
import com.lastmetro.patterns.RunningState;

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

    public Commuter(double x, double groundY) {
        super(x, groundY - STANDING_HEIGHT, 50.0, STANDING_HEIGHT);
        this.groundY = groundY;
        this.currentState = new RunningState();
    }

    @Override
    public void update(double deltaTime) {
        currentState.update(this, deltaTime);
    }

    public void onJumpPressed() {
        currentState.handleJumpPress(this);
    }

    public void onJumpReleased() {
        currentState.handleJumpRelease(this);
    }

    public void onSlideChanged(boolean held) {
        this.slideHeld = held;
        currentState.handleSlide(this, held);
    }

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