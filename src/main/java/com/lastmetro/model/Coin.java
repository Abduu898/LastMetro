package com.lastmetro.model;

/** A gold Métro token. Collect the whole trail to build a score multiplier. */
public final class Coin extends Entity {
    public static final int VALUE = 25;
    public Coin(double x, double y) { super(x, y, 24, 28); }
    public void scroll(double speed, double dt) { x -= speed * dt; }
    @Override public void update(double dt) { scroll(GameWorld.BASE_SCROLL_SPEED, dt); }
}
