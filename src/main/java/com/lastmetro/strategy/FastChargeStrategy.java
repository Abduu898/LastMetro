package com.lastmetro.patterns.strategy;

import com.lastmetro.model.Hazard;

public class FastChargeStrategy implements MovementStrategy {
    // A small speed edge keeps the scooter distinct without overtaking a
    // previous obstacle and creating an unavoidable cluster.
    private static final double SPEED_MULTIPLIER = 1.05;

    @Override
    public void move(Hazard hazard, double baseScrollSpeed, double deltaTime) {
        hazard.setX(hazard.getX() - (baseScrollSpeed * SPEED_MULTIPLIER) * deltaTime);
    }
}
