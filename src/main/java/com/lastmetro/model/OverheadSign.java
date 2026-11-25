package com.lastmetro.model;

import com.lastmetro.strategy.StaticGroundStrategy;

/** An overhead obstruction with a visible opening at slide height. */
public final class OverheadSign extends Hazard {
    public OverheadSign(double x, double groundY) {
        super(x, groundY - 96, 74, 42, new StaticGroundStrategy());
    }

    @Override public String onCollide(Commuter player) {
        player.applySlowDebuff(.65);
        return "SLIDE UNDER THE SIGN";
    }

    @Override public String getType() { return "OVERHEAD_SIGN"; }
}
