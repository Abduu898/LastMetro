package com.lastmetro.model;

import com.lastmetro.strategy.AerialSwoopStrategy;

public class Pigeon extends Hazard {
    public Pigeon(double spawnX, double groundY) {
        super(spawnX, groundY - 95, 45, 35, new AerialSwoopStrategy(groundY - 95));
    }

    @Override
    public String onCollide(Commuter player) {
        player.applySlowDebuff(2.0);
        return "Un pigeon parisien t'a attaqué en piqué !";
    }

    @Override
    public String getType() { return "PIGEON"; }
}
