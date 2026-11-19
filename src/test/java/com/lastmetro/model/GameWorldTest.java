package com.lastmetro.model;

import com.lastmetro.model.GameWorld.Mode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameWorldTest {
    @Test
    void catchTheMetroModeEndsWithAWinWhenTheCommuterKeepsAHeart() {
        GameWorld world = new GameWorld(1280, 580, GameWorld.Mode.CATCH_THE_METRO);

        world.update(43.0);
        world.update(0.6);
        world.update(0.01);
        assertTrue(world.isFinaleActive());
        assertFalse(world.isGameOver());

        world.update(1.0);
        world.update(0.1);
        world.update(0.5);
        world.update(0.5);
        world.update(1.0);

        assertTrue(world.isGameOver());
        assertTrue(world.isPlayerWon());
        assertTrue(world.getEndMessage().contains("attrapé"));
    }

    @Test
    void catchTheMetroModeLosesIfTimeRunsOutBeforeTheGoal() {
        GameWorld world = new GameWorld(1280, 580, GameWorld.Mode.CATCH_THE_METRO);

        world.update(GameWorld.INITIAL_TIME);

        assertTrue(world.isGameOver());
        assertFalse(world.isPlayerWon());
        assertTrue(world.getEndMessage().contains("left before"));
    }

    @Test
    void endlessModeHasNoCountdownAndDistanceKeepsIncreasing() {
        GameWorld world = new GameWorld(1280, 580, GameWorld.Mode.ENDLESS);

        world.update(1.0);
        double firstDistance = world.getDistanceMeters();
        assertEquals(GameWorld.INITIAL_TIME, world.getTimeRemaining());
        assertTrue(firstDistance > 0.0);

        world.update(1.0);
        assertTrue(world.getDistanceMeters() > firstDistance * 2.0);
        assertEquals(GameWorld.INITIAL_TIME, world.getTimeRemaining());
        assertFalse(world.isGameOver());
    }

    @Test
    void hazardCadenceTightensGraduallyAndStopsAtAFairMinimumInBothModes() {
        assertEquals(GameWorld.INITIAL_HAZARD_INTERVAL_SECONDS,
                GameWorld.hazardSpawnInterval(Mode.CATCH_THE_METRO, GameWorld.INITIAL_TIME, 0));
        assertEquals(GameWorld.MIN_HAZARD_INTERVAL_SECONDS,
                GameWorld.hazardSpawnInterval(Mode.CATCH_THE_METRO, 0, 0));

        double earlyEndless = GameWorld.hazardSpawnInterval(Mode.ENDLESS, GameWorld.INITIAL_TIME, 0);
        double midEndless = GameWorld.hazardSpawnInterval(Mode.ENDLESS, GameWorld.INITIAL_TIME, 500);
        double cappedEndless = GameWorld.hazardSpawnInterval(Mode.ENDLESS, GameWorld.INITIAL_TIME, 1000);
        assertTrue(earlyEndless > midEndless);
        assertTrue(midEndless > cappedEndless);
        assertEquals(GameWorld.MIN_HAZARD_INTERVAL_SECONDS, cappedEndless);
        assertEquals(cappedEndless,
                GameWorld.hazardSpawnInterval(Mode.ENDLESS, GameWorld.INITIAL_TIME, 5000));
    }

    @Test
    void collectingTicketAddsBonusPointsAndResetClearsIt() {
        GameWorld world = new GameWorld(1280, 580, GameWorld.Mode.ENDLESS);
        world.getTickets().add(new Ticket(150, 520));

        world.update(0.01);

        assertEquals(1, world.getTicketsCollected());
        assertTrue(world.getScore() >= Ticket.SCORE_VALUE);
        assertTrue(world.getTickets().isEmpty());

        world.reset();
        assertEquals(0, world.getTicketsCollected());
        assertEquals(0, world.getScore());
        assertTrue(world.getTickets().isEmpty());
    }

    @Test
    void collisionRemovesOneHeartAndGracePeriodPreventsRapidDamage() {
        GameWorld world = new GameWorld(1280, 580, Mode.ENDLESS);

        addGroundCollision(world, 0.01);
        assertEquals(2, world.getHearts());
        assertTrue(world.isInvulnerable());

        addGroundCollision(world, 0.2);
        assertEquals(2, world.getHearts());
    }

    @Test
    void losingAllHeartsEndsAnEndlessRun() {
        GameWorld world = new GameWorld(1280, 580, Mode.ENDLESS);

        addGroundCollision(world, 1.01);
        addGroundCollision(world, 1.01);
        addGroundCollision(world, 1.01);

        assertEquals(0, world.getHearts());
        assertTrue(world.isGameOver());
        assertFalse(world.isPlayerWon());
    }

    private static void addGroundCollision(GameWorld world, double deltaTime) {
        double hazardX = world.getPlayer().getX() + GameWorld.BASE_SCROLL_SPEED * deltaTime;
        world.getHazards().add(new DogPoop(hazardX, 580));
        world.update(deltaTime);
    }
}
