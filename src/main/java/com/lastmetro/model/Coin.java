package com.lastmetro.model;

/** A gold Métro token. Collect the whole trail to build a score multiplier. */
public final class Coin extends Entity {
    public static final int VALUE = 25;
    private final boolean landingMarker;

    public Coin(double x, double y) { this(x, y, false); }

    Coin(double x, double y, boolean landingMarker) {
        super(x, y, 24, 28);
        this.landingMarker = landingMarker;
    }

    boolean isLandingMarker() { return landingMarker; }
    public void scroll(double speed, double dt) { x -= speed * dt; }
    @Override public void update(double dt) { scroll(GameWorld.BASE_SCROLL_SPEED, dt); }
}
