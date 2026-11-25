package com.lastmetro.model;

import com.lastmetro.patterns.factory.HazardFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinishDeadlineTest {
    @Test void crossingTheGoalStartsTheFinaleInTheSameSimulationStep() {
        var world=new GameWorld(1280,576,new HazardFactory(){
            @Override public Hazard createRandomHazard(double x,double y){return new EScooter(1e9,y);}
        },GameWorld.Mode.CATCH_THE_METRO);
        for(int i=0;i<7000 && !world.isFinaleActive();i++) {
            world.update(1.0/120);
            if(world.getDistanceMeters()>=GameWorld.NORMAL_GOAL_METERS)assertTrue(world.isFinaleActive());
        }
        assertTrue(world.isFinaleActive());assertEquals(500,world.getDistanceMeters());
        assertTrue(world.getTimeRemaining()>0);
    }
}
