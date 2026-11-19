package com.lastmetro.model;

import com.lastmetro.patterns.factory.HazardFactory;
import com.lastmetro.patterns.observer.GameObserver;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Repeatable autoplay runs used to catch impossible hazard sequences and timer regressions. */
class GameWorldBalanceTest {
    private static final double STEP = 1.0 / 60.0;

    @Test
    void catchTheMetroIsReachableAcrossFixedHazardSeeds() {
        for (long seed = 0; seed < 8; seed++) {
            GameWorld world = new GameWorld(1280, 580,
                    new HazardFactory(new Random(seed)), GameWorld.Mode.CATCH_THE_METRO);

            List<String> events = runAutopilot(world, 55.0);

            assertTrue(world.isPlayerWon(), "Route should be reachable with pilot on seed " + seed
                    + "; ended at " + Math.round(world.getDistanceMeters()) + " m, hearts "
                    + world.getHearts() + ", time " + world.getTimeRemaining() + ", events " + events);
            assertTrue(world.getHearts() > 0, "Pilot should avoid losing all hearts on seed " + seed);
        }
    }

    @Test
    void endlessDifficultyRampKeepsEightSeededRunsAliveForOneMinute() {
        for (long seed = 0; seed < 8; seed++) {
            GameWorld world = new GameWorld(1280, 580,
                    new HazardFactory(new Random(seed)), GameWorld.Mode.ENDLESS);

            List<String> events = runAutopilot(world, 60.0);

            assertFalse(world.isGameOver(), "Endless run should remain playable on seed " + seed + "; events " + events);
            assertTrue(world.getDistanceMeters() >= 900.0,
                    "Endless speed curve should reach its first ramp milestone on seed " + seed);
        }
    }

    private static List<String> runAutopilot(GameWorld world, double maxSeconds) {
        List<String> events = new ArrayList<>();
        world.addObserver(new GameObserver() {
            @Override
            public void onSarcasticEvent(String message) {
                if (message.contains("Cœurs")) events.add(message + " @ "
                        + Math.round(world.getDistanceMeters()) + "m playerY="
                        + Math.round(world.getPlayer().getY()) + " state="
                        + world.getPlayer().getCurrentState().getStateName());
            }

            @Override
            public void onFinaleTriggered(String reason) { }
        });
        Hazard target = null;
        boolean actionUsesJumpKey = false;
        int steps = (int) Math.ceil(maxSeconds / STEP);

        for (int frame = 0; frame < steps && !world.isGameOver(); frame++) {
            if (target == null) {
                target = world.getHazards().stream()
                        .filter(hazard -> !hazard.isTriggered())
                        .filter(hazard -> hazard.getX() + hazard.getWidth() >= world.getPlayer().getX())
                        .min(Comparator.comparingDouble(Hazard::getX))
                        .orElse(null);
                if (target != null) {
                    double scrollSpeed = GameWorld.BASE_SCROLL_SPEED;
                    if (world.getMode() == GameWorld.Mode.ENDLESS) {
                        scrollSpeed *= 1.0 + Math.min(world.getDistanceMeters() / 1000.0, 1.5);
                    }
                    if (world.getPlayer().isSlowed()) scrollSpeed *= 0.65;
                    double multiplier = target.getType().equals("E_SCOOTER") ? 1.05 : 1.0;
                    double reactionDistance = scrollSpeed * multiplier * 0.42;
                    if (target.getX() - world.getPlayer().getX() > reactionDistance) target = null;
                    else {
                        boolean wantsSlide = target.getType().equals("PIGEON");
                        actionUsesJumpKey = wantsSlide == world.getPlayer().isConfused();
                        events.add((wantsSlide ? "slide " : "jump ") + target.getType() + " "
                                + Math.round(world.getDistanceMeters()) + "m gap "
                                + Math.round(target.getX() - world.getPlayer().getX()));
                        if (actionUsesJumpKey) world.getPlayer().onJumpPressed();
                        else world.getPlayer().onSlideChanged(true);
                    }
                }
            }

            if (target != null) {
                if (target.getX() + target.getWidth() < world.getPlayer().getX()) {
                    if (actionUsesJumpKey) world.getPlayer().onJumpReleased();
                    else world.getPlayer().onSlideChanged(false);
                    target = null;
                }
            }

            world.update(STEP);
        }
        return events;
    }
}
