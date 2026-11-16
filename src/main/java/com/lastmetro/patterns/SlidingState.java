package com.lastmetro.patterns;

import com.lastmetro.model.Commuter;

public class SlidingState implements PlayerState {
    @Override
    public void handleJumpPress(Commuter player) {
        // Jump-cancel straight out of a slide
        player.exitSlideHitbox();
        player.setVelocityY(Commuter.JUMP_FORCE);
        player.setState(new JumpingState());
    }

    @Override
    public void handleJumpRelease(Commuter player) {
        // Not jumping
    }

    @Override
    public void handleSlide(Commuter player, boolean sliding) {
        if (!sliding) {
            player.exitSlideHitbox();
            player.setState(new RunningState());
        }
    }

    @Override
    public void update(Commuter player, double deltaTime) {
        player.snapToGround();
    }

    @Override
    public String getStateName() {
        return "SLIDING";
    }
}