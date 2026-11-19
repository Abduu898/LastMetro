package com.lastmetro.model;

/** A collectible métro ticket carried by the scrolling game world. */
public class Ticket extends Entity {
    public static final int SCORE_VALUE = 100;

    public Ticket(double x, double y) {
        super(x, y, 24.0, 30.0);
    }

    public void updateWithScroll(double scrollSpeed, double deltaTime) {
        x -= scrollSpeed * deltaTime;
    }

    @Override
    public void update(double deltaTime) {
        updateWithScroll(GameWorld.BASE_SCROLL_SPEED, deltaTime);
    }

    public boolean isOffScreen() {
        return x + width < 0;
    }
}
