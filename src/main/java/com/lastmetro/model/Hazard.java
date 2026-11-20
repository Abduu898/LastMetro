package com.lastmetro.model;

import com.lastmetro.strategy.MovementStrategy;

public abstract class Hazard extends Entity {
    protected final MovementStrategy strategy;
    protected boolean triggered = false;

    public Hazard(double x, double y, double width, double height, MovementStrategy strategy) {
        super(x, y, width, height);
        this.strategy = strategy;
    }

    public void updateWithScroll(double baseScrollSpeed, double deltaTime) {
        strategy.move(this, baseScrollSpeed, deltaTime);
    }

    @Override
    public void update(double deltaTime) {
        updateWithScroll(450.0, deltaTime);
    }

    public abstract String onCollide(Commuter player);

    public boolean isOffScreen() {
        return (x + width) < 0;
    }

    public boolean isTriggered() { return triggered; }
    public void setTriggered(boolean triggered) { this.triggered = triggered; }
    public abstract String getType();
}