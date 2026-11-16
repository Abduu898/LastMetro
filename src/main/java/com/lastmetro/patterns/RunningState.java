package com.lastmetro.patterns;

import com.lastmetro.model.Commuter;

public class RunningState implements PlayerState {
    @Override
    public void handleJumpPress(Commuter player) {
        player.setVelocityY(Commuter.JUMP_FORCE);
        player.setState(new JumpingState());
    }

    @Override
    public void handleJumpRelease(Commuter player) {
        // Already grounded
    }

    @Override
    public void handleSlide(Commuter player, boolean sliding) {
        if (sliding) {
            player.enterSlideHitbox();
            player.setState(new SlidingState());
        }
    }

    @Override
    public void update(Commuter player, double deltaTime) {
        player.snapToGround();
    }

    @Override
    public String getStateName() {
        return "RUNNING";
    }
}