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
            player.setVelocityY(Math.max(player.getVelocityY(), -300.0));
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
        // A readable apex followed by a decisive, controllable descent.
        double gravity = (player.getVelocityY() > 0)
                ? Commuter.GRAVITY * Commuter.FALL_GRAVITY_MULTIPLIER
                : Commuter.GRAVITY;

        double before = player.getVelocityY();
        player.setVelocityY(Math.min(Commuter.FAST_FALL_SPEED, before + gravity * deltaTime));
        player.setY(player.getY() + (before + player.getVelocityY()) * .5 * deltaTime);

        if (player.isAtOrBelowGround()) {
            player.snapToGround();
            player.setVelocityY(0);
            if (player.launchBufferedJumpIfReady()) {
                return;
            } else if (player.isSlideHeld()) {
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
