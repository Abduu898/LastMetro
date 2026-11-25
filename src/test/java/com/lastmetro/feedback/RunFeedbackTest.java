package com.lastmetro.feedback;

import com.lastmetro.model.*;
import com.lastmetro.patterns.factory.HazardFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.lastmetro.feedback.RunFeedback.Event.*;

class RunFeedbackTest {
    private GameWorld quiet(GameWorld.Mode mode) {
        return new GameWorld(1280, 554, new HazardFactory() {
            @Override public Pattern choosePattern(int stage, boolean opening, Pattern previous) { return Pattern.EMPTY; }
        }, mode);
    }

    @Test void jumpLandingPickupAndDamageAreOneShotEvents() {
        var world = quiet(GameWorld.Mode.ENDLESS);
        var feedback = new RunFeedback();
        feedback.update(world, .01);
        world.getPlayer().onJumpPressed(); world.update(.02);
        assertTrue(feedback.update(world, .02).contains(JUMP));
        assertFalse(feedback.update(world, .02).contains(JUMP));
        world.getPlayer().onJumpReleased();
        for (int i = 0; i < 100; i++) world.update(.01);
        assertTrue(feedback.update(world, .02).contains(LAND));
        assertFalse(feedback.update(world, .02).contains(LAND));
        world.getTickets().add(new Ticket(154, 480)); world.update(.001);
        assertTrue(feedback.update(world, .001).contains(TICKET));
        assertFalse(feedback.update(world, .001).contains(TICKET));
        world.getHazards().add(new EScooter(154, 554)); world.update(.001);
        assertTrue(feedback.update(world, .001).contains(HIT));
        assertFalse(feedback.update(world, .001).contains(HIT));
    }

    @Test void warningAndTrainEventsDoNotRepeatAndResetAllowsAnotherRun() {
        var world = quiet(GameWorld.Mode.CATCH_THE_METRO);
        var feedback = new RunFeedback();
        while (world.getTimeRemaining() > 10) world.update(.01);
        assertTrue(feedback.update(world, .01).contains(HURRY));
        assertFalse(feedback.update(world, .01).contains(HURRY));
        int arrived = 0, doors = 0, departed = 0, won = 0;
        for (int i = 0; i < 1500; i++) {
            world.update(.01);
            var events = feedback.update(world, .01);
            if (events.contains(TRAIN_ARRIVE)) arrived++;
            if (events.contains(DOORS)) doors++;
            if (events.contains(TRAIN_DEPART)) departed++;
            if (events.contains(WIN)) won++;
        }
        assertEquals(1, arrived); assertEquals(1, doors); assertEquals(1, departed); assertEquals(1, won);
        world.reset(); feedback.reset();
        world.getPlayer().onJumpPressed(); world.update(.01);
        assertTrue(feedback.update(world, .01).contains(JUMP));
    }

    @Test void endlessHasNoDepartureWarningAndDefeatOnlyAnnouncesOnce() {
        var world = quiet(GameWorld.Mode.ENDLESS);
        var feedback = new RunFeedback();
        for (int i = 0; i < 3; i++) {
            world.getHazards().clear();
            world.getHazards().add(new EScooter(154, 554)); world.update(.001);
            var events = feedback.update(world, .001);
            assertFalse(events.contains(HURRY));
            if (i == 2) assertTrue(events.contains(LOSE));
            else { world.getHazards().clear(); world.update(1.1); }
        }
        assertFalse(feedback.update(world, .01).contains(LOSE));
    }
}
