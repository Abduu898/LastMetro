package com.lastmetro.patterns.strategy;

import com.lastmetro.model.Hazard;

public class StaticGroundStrategy implements MovementStrategy {
    @Override
    public void move(Hazard hazard, double baseScrollSpeed, double deltaTime) {
        hazard.setX(hazard.getX() - baseScrollSpeed * deltaTime);
    }
}