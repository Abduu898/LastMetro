package com.lastmetro.model;

import com.lastmetro.strategy.StaticGroundStrategy;

/** Distinct, readable ground obstacles; their hitboxes follow the visible prop. */
public final class StreetObstacle extends Hazard {
    private final String type;
    public StreetObstacle(double x, double ground, boolean crate) {
        super(x, ground - (crate ? 58 : 54), crate ? 58 : 84, crate ? 58 : 54, new StaticGroundStrategy());
        type = crate ? "CRATE" : "BARRIER";
    }
    @Override public String getType() { return type; }
    @Override public String onCollide(Commuter player) {
        player.applySlowDebuff(.65);
        return type.equals("CRATE") ? "Watch the crates! Jump to clear them." : "Roadworks! Jump over the barrier.";
    }
}
