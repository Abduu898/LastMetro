package com.lastmetro.patterns.factory;

import com.lastmetro.model.DogPoop;
import com.lastmetro.model.EScooter;
import com.lastmetro.model.Hazard;
import com.lastmetro.model.Pigeon;
import com.lastmetro.model.StreetObstacle;

import java.util.Random;

/**
 * Factory Pattern responsible for instantiating Parisian obstacles.
 */
public class HazardFactory {
    private final Random random;

    public HazardFactory() {
        this.random = new Random();
    }

    public HazardFactory(Random random) {
        this.random = random;
    }

    public Hazard createRandomHazard(double spawnX, double groundY) {
        int roll = random.nextInt(5);
        return switch (roll) {
            case 0 -> new DogPoop(spawnX, groundY);
            case 1 -> new EScooter(spawnX, groundY);
            case 2 -> new Pigeon(spawnX, groundY);
            case 3 -> new StreetObstacle(spawnX, groundY, false);
            default -> new StreetObstacle(spawnX, groundY, true);
        };
    }
}
