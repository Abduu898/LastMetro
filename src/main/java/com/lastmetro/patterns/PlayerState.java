package com.lastmetro.patterns;

import com.lastmetro.model.Commuter;

/**
 * State Pattern interface governing player movement and hitbox behavior.
 */
public interface PlayerState {
    void handleJumpPress(Commuter player);
    void handleJumpRelease(Commuter player);
    void handleSlide(Commuter player, boolean sliding);
    void update(Commuter player, double deltaTime);
    String getStateName();
}