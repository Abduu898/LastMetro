package com.lastmetro.model;

import com.lastmetro.patterns.factory.HazardFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RouteTimelineTest {
    private GameWorld quiet() {
        return new GameWorld(1280,576,new HazardFactory() {
            @Override public Hazard createRandomHazard(double x,double y) { return new EScooter(1e9,y); }
        },GameWorld.Mode.CATCH_THE_METRO);
    }
    private void advance(GameWorld world, double seconds) {
        for(int i=0;i<(int)Math.round(seconds*120);i++) world.update(1.0/120);
    }
    @Test void transfersPauseCountdownDistanceAndInputsAndSwapExactlyOnce() {
        var world=quiet(); advance(world,20);
        assertTrue(world.isTransferring());assertEquals(0,world.getRouteStage());
        assertEquals(25,world.getTimeRemaining(),.001);assertTrue(world.getHazards().isEmpty());
        double distance=world.getDistanceMeters();advance(world,1.2);
        assertEquals(.5,world.getTransferProgress(),.001);assertEquals(distance,world.getDistanceMeters(),.001);
        assertEquals(25,world.getTimeRemaining(),.001);advance(world,1.2);
        assertFalse(world.isTransferring());assertEquals(1,world.getRouteStage());
        assertEquals(0,world.getStageProgress(),.001);advance(world,13);
        assertTrue(world.isTransferring());assertEquals(12,world.getTimeRemaining(),.001);
        advance(world,2.4);assertEquals(2,world.getRouteStage());assertFalse(world.isTransferring());
    }
    @Test void timeAcrossBothTransfersIsConservedAndRetryRestartsTheRoute() {
        var world=quiet();advance(world,40);
        assertEquals(35.2,world.getActiveElapsed(),.001);assertEquals(2,world.getRouteStage());
        world.reset();assertEquals(0,world.getRouteStage());assertFalse(world.isTransferring());
        assertEquals(45,world.getTimeRemaining());assertEquals(0,world.getStageProgress());
    }
}
