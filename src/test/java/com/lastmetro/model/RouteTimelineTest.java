package com.lastmetro.model;

import com.lastmetro.patterns.factory.HazardFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RouteTimelineTest {
    private GameWorld quiet() {
        return new GameWorld(1280,576,new HazardFactory() {
            @Override public Pattern choosePattern(int stage, boolean opening, Pattern previous) { return Pattern.EMPTY; }
        },GameWorld.Mode.CATCH_THE_METRO);
    }
    private void advance(GameWorld world, double seconds) {
        for(int i=0;i<(int)Math.round(seconds*120);i++) world.update(1.0/120);
    }
    @Test void transfersPauseCountdownDistanceAndSwapAtDistanceEvenAfterSlowdown() {
        var world = quiet();
        world.getPlayer().applySlowDebuff(4);
        advance(world, 20);
        assertFalse(world.isTransferring());
        while (!world.isTransferring()) world.update(1.0 / 120);
        assertEquals(RouteTimeline.FIRST_STREET_END, world.getDistanceMeters(), .001);
        assertEquals(0, world.getRouteStage());
        double time = world.getTimeRemaining();
        advance(world, 1.2);
        assertEquals(.5, world.getTransferProgress(), .01);
        assertEquals(time, world.getTimeRemaining(), .001);
        advance(world, 1.2);
        assertEquals(1, world.getRouteStage());
        assertEquals(0, world.getStageProgress(), .002);
        while (!world.isTransferring()) world.update(1.0 / 120);
        assertEquals(RouteTimeline.SECOND_STREET_END, world.getDistanceMeters(), .001);
    }

    @Test void retriesUseTheShortTransferAndResetTheRoute() {
        var world = quiet();
        world.reset();
        assertEquals(GameWorld.INITIAL_TIME, world.getTimeRemaining());
        assertEquals(0, world.getStageProgress());
        while (!world.isTransferring()) world.update(1.0 / 120);
        advance(world, .45);
        assertEquals(.5, world.getTransferProgress(), .01);
        advance(world, .45);
        assertEquals(1, world.getRouteStage());
        assertFalse(world.isTransferring());
    }
}
