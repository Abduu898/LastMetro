package com.lastmetro.model;

import com.lastmetro.patterns.factory.HazardFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArcadeRunTest {
    private GameWorld quiet() {
        return new GameWorld(1280, 576, new HazardFactory() {
            @Override public Hazard createRandomHazard(double x, double y) { return new EScooter(1e9, y); }
        }, GameWorld.Mode.ENDLESS);
    }
    @Test void sevenCoinsBuildMultiplierAndRetryClearsAllCollectibles() {
        var world = quiet();
        for (int i = 0; i < 7; i++) {
            world.getCoins().add(new Coin(154, 520)); world.update(.001);
        }
        assertEquals(7, world.getCoinsCollected()); assertEquals(7, world.getCombo());
        assertEquals(2, world.getMultiplier()); assertTrue(world.getScore() >= 200);
        world.getCoins().add(new Coin(1200, 500)); world.getTickets().add(new Ticket(900, 480));
        world.reset();
        assertTrue(world.getCoins().isEmpty()); assertTrue(world.getTickets().isEmpty());
        assertEquals(0, world.getCoinsCollected()); assertEquals(1, world.getMultiplier()); assertEquals(0, world.getScore());
    }
    @Test void comboTimesOutAndDamageBreaksTheStreak() {
        var world = quiet();
        world.getCoins().add(new Coin(154, 520)); world.update(.001);
        assertEquals(1, world.getCombo());
        for (int i = 0; i < 290; i++) world.update(.01);
        assertEquals(0, world.getCombo());
        world.getCoins().add(new Coin(154, 520)); world.update(.001);
        world.getHazards().add(new StreetObstacle(154, 576, true)); world.update(.001);
        assertEquals(0, world.getCombo()); assertEquals(2, world.getHearts());
    }
    @Test void coinArcsStayReachableAndTheirPopulationRemainsBounded() {
        var world = quiet();
        int peak = 0;
        for (int i = 0; i < 6000; i++) {
            world.update(.01); peak = Math.max(peak, world.getCoins().size());
            for (var coin : world.getCoins()) assertTrue(coin.getY() >= 576 - 161 && coin.getY() <= 576 - 53);
        }
        assertTrue(peak > 7); assertTrue(peak < 30);
    }
    @Test void barrierAndCrateCanBothBeClearedByTheTunedJump() {
        for (boolean crate : new boolean[] {false, true}) {
            var world = quiet();
            world.update(.6); world.getHazards().clear();
            world.getHazards().add(new StreetObstacle(350, 576, crate)); world.getPlayer().onJumpPressed();
            for (int i = 0; i < 70; i++) world.update(1.0 / 60);
            assertEquals(3, world.getHearts());
        }
    }
    @Test void navigoPickupPullsNearbyCoinsAndExpiresOnRetry() {
        var world = quiet();
        world.getTickets().add(new Ticket(154, 500)); world.update(.001);
        assertTrue(world.getMagnetTime() > 4.9);
        world.getCoins().add(new Coin(330, 440));
        for (int i = 0; i < 45; i++) world.update(.01);
        assertTrue(world.getCoinsCollected() > 0);
        world.reset(); assertEquals(0, world.getMagnetTime());
    }
}
