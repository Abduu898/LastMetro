package com.lastmetro.patterns.strategy;

import com.lastmetro.model.Hazard;

public class AerialSwoopStrategy implements MovementStrategy {
    private double elapsed = 0;
    private final double baseY;

    public AerialSwoopStrategy(double baseY) {
        this.baseY = baseY;
    }

    @Override
    public void move(Hazard hazard, double baseScrollSpeed, double deltaTime) {
        elapsed += deltaTime;
        // Keep the same horizontal pace as the street; only the swoop varies.
        hazard.setX(hazard.getX() - baseScrollSpeed * deltaTime);
        hazard.setY(baseY + Math.sin(elapsed * 8.0) * 10.0);
    }
}
