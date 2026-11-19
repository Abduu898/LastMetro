package com.lastmetro.strategy;

import com.lastmetro.model.EScooter;
import com.lastmetro.model.Pigeon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HazardMovementStrategyTest {
    @Test
    void scooterIsOnlySlightlyFasterSoItCannotRushPastNearbyHazards() {
        EScooter scooter = new EScooter(1000, 580);

        scooter.updateWithScroll(400, 0.5);

        assertEquals(790.0, scooter.getX(), 0.0001);
    }

    @Test
    void pigeonSwoopsVerticallyWithoutChangingItsHorizontalPace() {
        Pigeon pigeon = new Pigeon(1000, 580);

        pigeon.updateWithScroll(400, 0.5);

        assertEquals(800.0, pigeon.getX(), 0.0001);
        assertNotEquals(580.0 - 95.0, pigeon.getY());
        assertTrue(Math.abs(pigeon.getY() - (580.0 - 95.0)) <= 10.0);
    }
}
