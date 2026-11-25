package com.lastmetro.model;

import com.lastmetro.patterns.factory.HazardFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinishDeadlineTest {
    @Test void crossingTheGoalStartsTheFinaleInTheSameSimulationStep() {
        var world=new GameWorld(1280,576,new HazardFactory(){
            @Override public Pattern choosePattern(int stage, boolean opening, Pattern previous) { return Pattern.EMPTY; }
        },GameWorld.Mode.CATCH_THE_METRO);
        for(int i=0;i<7000 && !world.isFinaleActive();i++) {
            world.update(1.0/120);
            if(world.getDistanceMeters()>=GameWorld.NORMAL_GOAL_METERS)assertTrue(world.isFinaleActive());
        }
        assertTrue(world.isFinaleActive());assertEquals(500,world.getDistanceMeters());
        assertTrue(world.getTimeRemaining() > 5 && world.getTimeRemaining() < 7);
        assertEquals(3, world.getCleanSections());
        world.update(5);
        assertEquals("GOLD", world.getMedal());
    }

    @Test void twoSeparateScooterHitsLeaveEnoughTimeToBoard() {
        var world = new GameWorld(1280, 576, new HazardFactory() {
            @Override public Pattern choosePattern(int stage, boolean opening, Pattern previous) { return Pattern.EMPTY; }
        }, GameWorld.Mode.CATCH_THE_METRO);
        world.getHazards().add(new EScooter(166, 576));
        world.update(4);
        world.getHazards().add(new EScooter(166, 576));
        world.update(54);
        assertEquals(2, world.getHitsTaken());
        assertTrue(world.isPlayerWon());
        assertEquals("SILVER", world.getMedal());
        assertTrue(world.getTimeRemaining() >= 3);
    }
}
