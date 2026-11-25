package com.lastmetro.model;

import com.lastmetro.strategy.FastChargeStrategy;

public class EScooter extends Hazard {
    public EScooter(double spawnX, double groundY) {
        super(spawnX, groundY - 45, 70, 45, new FastChargeStrategy());
    }

    @Override
    public String onCollide(Commuter player) {
        player.applySlowDebuff(2.5);
        return "Scooter collision! Slowed down for 2.5 seconds.";
    }

    @Override
    public String getType() { return "E_SCOOTER"; }
}
