package com.lastmetro.feedback;

import com.lastmetro.model.GameWorld;
import java.util.ArrayList;
import java.util.List;

/** Converts simulation changes into one-shot presentation events, independent of audio hardware. */
public final class RunFeedback {
    public enum Event { JUMP, LAND, STEP, SLIDE, TICKET, COIN, HEART, HIT, WARNING, HURRY,
        TRAIN_ARRIVE, DOORS, TRAIN_DEPART, WIN, LOSE }
    private int tickets, second = 45;
    private int coins;
    private int heartsCollected, hits;
    private boolean airborne, sliding, hurry, finale, doors, departure, ended;
    private double stepClock;

    public List<Event> update(GameWorld world, double dt) {
        var events = new ArrayList<Event>();
        var player = world.getPlayer();
        boolean inAir = player.getCurrentState().getStateName().equals("JUMPING");
        boolean inSlide = player.getCurrentState().getStateName().equals("SLIDING");
        if (!world.isGameOver() && !world.isTransferring()) {
            if (inAir && !airborne) events.add(Event.JUMP);
            if (!inAir && airborne) events.add(Event.LAND);
            if (inSlide && !sliding) events.add(Event.SLIDE);
            stepClock -= dt;
            if (!inAir && !inSlide && stepClock <= 0) {
                events.add(Event.STEP);
                stepClock = .225 / Math.max(.65, world.getCurrentScrollSpeed() / GameWorld.BASE_SCROLL_SPEED);
            }
        }
        if (world.getHitsTaken() > hits) events.add(Event.HIT);
        if (world.getHeartsCollected() > heartsCollected) events.add(Event.HEART);
        if (world.getTicketsCollected() > tickets) events.add(Event.TICKET);
        if (world.getCoinsCollected() > coins) events.add(Event.COIN);
        int now = (int) Math.ceil(world.getTimeRemaining());
        if (world.getMode() == GameWorld.Mode.CATCH_THE_METRO && !world.isFinaleActive() && !world.isGameOver()) {
            if (now <= 10 && now > 0 && now != second) events.add(Event.WARNING);
            if (now <= 10 && !hurry) { events.add(Event.HURRY); hurry = true; }
        }
        if (world.isFinaleActive() && !finale) { events.add(Event.TRAIN_ARRIVE); finale = true; }
        if (world.getMetroDoorProgress() > 0 && !doors) { events.add(Event.DOORS); doors = true; }
        if (world.getMetroDoorProgress() >= 1 && !departure) { events.add(Event.TRAIN_DEPART); departure = true; }
        if (world.isGameOver() && !ended) { events.add(world.isPlayerWon() ? Event.WIN : Event.LOSE); ended = true; }
        tickets = world.getTicketsCollected(); second = now;
        coins = world.getCoinsCollected();
        heartsCollected = world.getHeartsCollected(); hits = world.getHitsTaken();
        airborne = inAir; sliding = inSlide;
        return List.copyOf(events);
    }

    public void reset() {
        tickets = coins = 0; second = 45; stepClock = 0;
        heartsCollected = hits = 0;
        airborne = sliding = hurry = finale = doors = departure = ended = false;
    }
}
