package com.lastmetro.model;

import com.lastmetro.strategy.StaticGroundStrategy;

public class DogPoop extends Hazard {
    public DogPoop(double spawnX, double groundY) {
        super(spawnX, groundY - 25, 35, 25, new StaticGroundStrategy());
    }

    @Override
    public String onCollide(Commuter player) {
        player.applySlowDebuff(.55);
        return "WATCH YOUR STEP";
    }

    @Override
    public String getType() { return "DOG_POOP"; }
}
