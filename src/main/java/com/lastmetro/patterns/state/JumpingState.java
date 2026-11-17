package com.lastmetro.patterns.state;

import com.lastmetro.model.Commuter;

public class JumpingState implements PlayerState {
    @Override
    public void handleJumpPress(Commuter player) {
        // Prevent double-jumping
    }

    @Override
    public void handleJumpRelease(Commuter player) {
        // Variable jump height: releasing SPACE early cuts upward momentum
        if (player.getVelocityY() < 0) {
            player.setVelocityY(player.getVelocityY() * 0.5);
        }
    }

    @Override
    public void handleSlide(Commuter player, boolean sliding) {
        // Fast-fall slam if DOWN is pressed mid-air
        if (sliding && player.getVelocityY() < Commuter.FAST_FALL_SPEED) {
            player.setVelocityY(Commuter.FAST_FALL_SPEED);
        }
    }

    @Override
    public void update(Commuter player, double deltaTime) {
        // Asymmetric gravity: fall 1.8x faster than rising for snappy arcade feel
        double gravity = (player.getVelocityY() > 0)
                ? Commuter.GRAVITY * 1.8
                : Commuter.GRAVITY;

        player.setVelocityY(player.getVelocityY() + gravity * deltaTime);
        player.setY(player.getY() + player.getVelocityY() * deltaTime);

        if (player.isAtOrBelowGround()) {
            player.snapToGround();
            player.setVelocityY(0);
            if (player.isSlideHeld()) {
                player.enterSlideHitbox();
                player.setState(new SlidingState());
            } else {
                player.setState(new RunningState());
            }
        }
    }

    @Override
    public String getStateName() {
        return "JUMPING";
    }
}