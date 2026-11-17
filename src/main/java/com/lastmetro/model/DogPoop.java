package com.lastmetro.model;

import com.lastmetro.patterns.strategy.StaticGroundStrategy;

public class DogPoop extends Hazard {
    public DogPoop(double spawnX, double groundY) {
        super(spawnX, groundY - 25, 35, 25, new StaticGroundStrategy());
    }

    @Override
    public String onCollide(Commuter player) {
        player.applyStenchDebuff(3.0);
        return "Bravo ! Souvenir du 18e : commandes inversées (3s) !";
    }

    @Override
    public String getType() { return "DOG_POOP"; }
}