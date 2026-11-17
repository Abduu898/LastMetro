package com.lastmetro.patterns.strategy;

import com.lastmetro.model.Hazard;

public class FastChargeStrategy implements MovementStrategy {
    @Override
    public void move(Hazard hazard, double baseScrollSpeed, double deltaTime) {
        hazard.setX(hazard.getX() - (baseScrollSpeed * 1.6) * deltaTime);
    }
}