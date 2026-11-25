package com.lastmetro.model;

import com.lastmetro.strategy.StaticGroundStrategy;

public class DogPoop extends Hazard {
    public DogPoop(double spawnX, double groundY) {
        super(spawnX, groundY - 25, 35, 25, new StaticGroundStrategy());
    }

    @Override
    public String onCollide(Commuter player) {
        player.applyStenchDebuff(3.0);
        return "Watch your step! Jump and slide are reversed for 3 seconds.";
    }

    @Override
    public String getType() { return "DOG_POOP"; }
}
