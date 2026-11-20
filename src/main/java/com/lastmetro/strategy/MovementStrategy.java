package com.lastmetro.strategy;

import com.lastmetro.model.Hazard;

public interface MovementStrategy {
    void move(Hazard hazard, double baseScrollSpeed, double deltaTime);
}