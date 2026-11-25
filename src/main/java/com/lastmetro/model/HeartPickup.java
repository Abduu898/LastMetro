package com.lastmetro.model;

/** A reachable recovery pickup. Full-health pickups give points instead of over-healing. */
public final class HeartPickup extends Entity {
    public static final int FULL_HEALTH_BONUS = 50;

    public HeartPickup(double x, double y) { super(x, y, 28, 28); }
    public void scroll(double speed, double dt) { x -= speed * dt; }
    @Override public void update(double dt) { scroll(GameWorld.BASE_SCROLL_SPEED, dt); }
}
