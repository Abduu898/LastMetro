package com.lastmetro.model;

import com.lastmetro.patterns.factory.HazardFactory;
import com.lastmetro.patterns.observer.GameObserver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Core pure-Java simulation holding the commuter, active hazards, countdown, and finale state.
 */
public class GameWorld {
    public static final double INITIAL_TIME = 45.0;
    public static final double BASE_SCROLL_SPEED = 460.0;

    private final double screenWidth;
    private final double groundY;
    private final Commuter player;
    private final HazardFactory hazardFactory;
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<GameObserver> observers = new ArrayList<>();

    private double timeRemaining = INITIAL_TIME;
    private double spawnCooldown = 1.2;
    private double distanceMeters = 0.0;
    private boolean finaleActive = false;
    private boolean gameOver = false;
    private double metroTrainX;
    private double metroDoorProgress = 0.0; // 0.0 = open, 1.0 = slammed shut

    public GameWorld(double screenWidth, double groundY) {
        this(screenWidth, groundY, new HazardFactory());
    }

    public GameWorld(double screenWidth, double groundY, HazardFactory hazardFactory) {
        this.screenWidth = screenWidth;
        this.groundY = groundY;
        this.player = new Commuter(150, groundY);
        this.hazardFactory = hazardFactory;
        this.metroTrainX = screenWidth + 50;
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void update(double deltaTime) {
        if (gameOver) return;

        // Scripted 00:00 unwinnable finale sequence
        if (finaleActive) {
            updateFinale(deltaTime);
            return;
        }

        timeRemaining = Math.max(0.0, timeRemaining - deltaTime);
        if (timeRemaining <= 0.0) {
            startFinale();
            return;
        }

        double currentSpeed = player.isSlowed() ? BASE_SCROLL_SPEED * 0.65 : BASE_SCROLL_SPEED;
        distanceMeters += (currentSpeed * deltaTime) / 40.0;

        player.update(deltaTime);

        // Spawn hazards faster as the clock approaches 00:00
        spawnCooldown -= deltaTime;
        if (spawnCooldown <= 0) {
            hazards.add(hazardFactory.createRandomHazard(screenWidth + 60, groundY));
            double urgency = 1.0 - (timeRemaining / INITIAL_TIME); // 0.0 -> 1.0
            spawnCooldown = Math.max(0.75, 1.65 - (urgency * 0.75));
        }

        // Move hazards & check AABB collisions
        Iterator<Hazard> it = hazards.iterator();
        while (it.hasNext()) {
            Hazard h = it.next();
            h.updateWithScroll(currentSpeed, deltaTime);

            if (!h.isTriggered() && player.intersects(h)) {
                h.setTriggered(true);
                String roast = h.onCollide(player);
                notifySarcasticEvent(roast);
            }

            if (h.isOffScreen()) {
                it.remove();
            }
        }
    }

    private void startFinale() {
        finaleActive = true;
        hazards.clear();
        player.onSlideChanged(false);
        notifySarcasticEvent("00:00 ! LE DERNIER MÉTRO EST À QUAI ! COURS !");
    }

    private void updateFinale(double deltaTime) {
        // Metro pulls into view on the right, slams doors shut, and departs
        double targetTrainX = 760.0;
        if (metroTrainX > targetTrainX && metroDoorProgress < 1.0) {
            metroTrainX = Math.max(targetTrainX, metroTrainX - 520.0 * deltaTime);
            player.setX(Math.min(680.0, player.getX() + 340.0 * deltaTime));
            player.update(deltaTime);
        } else if (metroDoorProgress < 1.0) {
            metroDoorProgress = Math.min(1.0, metroDoorProgress + 2.2 * deltaTime);
        } else {
            metroTrainX += 780.0 * deltaTime;
            if (metroTrainX > screenWidth + 100) {
                gameOver = true;
                notifyFinaleTriggered("Portes fermées à 2 cm de ton nez. Prochain départ : 05h30.");
            }
        }
    }

    public void reset() {
        hazards.clear();
        timeRemaining = INITIAL_TIME;
        spawnCooldown = 1.0;
        distanceMeters = 0.0;
        finaleActive = false;
        gameOver = false;
        metroTrainX = screenWidth + 50;
        metroDoorProgress = 0.0;
        player.setX(150);
        player.snapToGround();
        player.applyStenchDebuff(0);
        player.applySlowDebuff(0);
    }

    private void notifySarcasticEvent(String msg) {
        for (GameObserver obs : observers) obs.onSarcasticEvent(msg);
    }

    private void notifyFinaleTriggered(String reason) {
        for (GameObserver obs : observers) obs.onFinaleTriggered(reason);
    }

    public Commuter getPlayer() { return player; }
    public List<Hazard> getHazards() { return hazards; }
    public double getTimeRemaining() { return timeRemaining; }
    public double getDistanceMeters() { return distanceMeters; }
    public boolean isFinaleActive() { return finaleActive; }
    public boolean isGameOver() { return gameOver; }
    public double getMetroTrainX() { return metroTrainX; }
    public double getMetroDoorProgress() { return metroDoorProgress; }
}