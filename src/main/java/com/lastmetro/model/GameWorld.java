package com.lastmetro.model;

import com.lastmetro.patterns.factory.HazardFactory;
import com.lastmetro.patterns.observer.GameObserver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Core pure-Java simulation holding the commuter, active hazards, countdown, and finale state.
 */
public class GameWorld {
    public static final double INITIAL_TIME = 45.0;
    public static final double BASE_SCROLL_SPEED = 460.0;
    public static final double NORMAL_GOAL_METERS = 500.0;
    public static final int STARTING_HEARTS = 3;

    public enum Mode { CATCH_THE_METRO, ENDLESS }

    private final double screenWidth;
    private final double groundY;
    private final Commuter player;
    private final HazardFactory hazardFactory;
    private final Mode mode;
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<Ticket> tickets = new ArrayList<>();
    private final List<GameObserver> observers = new ArrayList<>();

    private double timeRemaining = INITIAL_TIME;
    private double spawnCooldown = 1.2;
    private double ticketSpawnCooldown = 3.0;
    private double distanceMeters = 0.0;
    private boolean finaleActive = false;
    private boolean gameOver = false;
    private double metroTrainX;
    private double metroDoorProgress = 0.0; // 0.0 = open, 1.0 = slammed shut
    private double invulnerabilityTimer = 0.0;
    private int hearts = STARTING_HEARTS;
    private int ticketsCollected = 0;
    private boolean playerWon = false;
    private String endMessage = "";

    public GameWorld(double screenWidth, double groundY) {
        this(screenWidth, groundY, new HazardFactory(), Mode.CATCH_THE_METRO);
    }

    public GameWorld(double screenWidth, double groundY, HazardFactory hazardFactory) {
        this(screenWidth, groundY, hazardFactory, Mode.CATCH_THE_METRO);
    }

    public GameWorld(double screenWidth, double groundY, Mode mode) {
        this(screenWidth, groundY, new HazardFactory(), mode);
    }

    public GameWorld(double screenWidth, double groundY, HazardFactory hazardFactory, Mode mode) {
        if (!Double.isFinite(screenWidth) || screenWidth <= 0.0) {
            throw new IllegalArgumentException("screenWidth must be finite and positive");
        }
        if (!Double.isFinite(groundY) || groundY <= 0.0) {
            throw new IllegalArgumentException("groundY must be finite and positive");
        }
        this.screenWidth = screenWidth;
        this.groundY = groundY;
        this.player = new Commuter(150, groundY);
        this.hazardFactory = Objects.requireNonNull(hazardFactory, "hazardFactory");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.metroTrainX = screenWidth + 50;
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void update(double deltaTime) {
        if (!Double.isFinite(deltaTime) || deltaTime < 0.0) {
            throw new IllegalArgumentException("deltaTime must be finite and non-negative");
        }
        if (gameOver) return;
        if (invulnerabilityTimer > 0) invulnerabilityTimer = Math.max(0, invulnerabilityTimer - deltaTime);

        // The last train arrives after the route is complete.
        if (finaleActive) {
            updateFinale(deltaTime);
            return;
        }

        if (mode == Mode.CATCH_THE_METRO) {
            timeRemaining = Math.max(0.0, timeRemaining - deltaTime);
            if (distanceMeters >= NORMAL_GOAL_METERS) {
                startFinale();
                return;
            }
            if (timeRemaining <= 0.0) {
                gameOver = true;
                endMessage = "The last métro left before you reached the platform.";
                notifyFinaleTriggered(endMessage);
                return;
            }
        }

        double rampedSpeed = mode == Mode.ENDLESS
                ? BASE_SCROLL_SPEED * (1.0 + Math.min(distanceMeters / 1000.0, 1.5))
                : BASE_SCROLL_SPEED;
        double currentSpeed = player.isSlowed() ? rampedSpeed * 0.65 : rampedSpeed;
        distanceMeters += (currentSpeed * deltaTime) / 40.0;

        player.update(deltaTime);

        // Spawn hazards faster as the clock approaches 00:00
        spawnCooldown -= deltaTime;
        if (spawnCooldown <= 0) {
            hazards.add(hazardFactory.createRandomHazard(screenWidth + 60, groundY));
            double urgency = mode == Mode.ENDLESS
                    ? Math.min(distanceMeters / 1000.0, 1.0)
                    : 1.0 - (timeRemaining / INITIAL_TIME);
            spawnCooldown = Math.max(0.75, 1.65 - (urgency * 0.75));
        }

        ticketSpawnCooldown -= deltaTime;
        if (ticketSpawnCooldown <= 0.0) {
            tickets.add(new Ticket(screenWidth + 60, groundY - Commuter.STANDING_HEIGHT - 45));
            ticketSpawnCooldown = 3.0;
        }

        // Move hazards & check AABB collisions
        Iterator<Hazard> it = hazards.iterator();
        while (it.hasNext()) {
            Hazard h = it.next();
            h.updateWithScroll(currentSpeed, deltaTime);

            if (!h.isTriggered() && player.intersects(h)) {
                h.setTriggered(true);
                if (invulnerabilityTimer <= 0.0) {
                    hearts = Math.max(0, hearts - 1);
                    invulnerabilityTimer = 1.0;
                    String roast = h.onCollide(player);
                    notifySarcasticEvent(roast + "  Cœurs : " + hearts + "/" + STARTING_HEARTS);
                    if (hearts == 0) {
                        gameOver = true;
                        endMessage = "Plus de cœurs. Le métro continue sans toi.";
                        notifyFinaleTriggered(endMessage);
                    }
                }
            }

            if (h.isOffScreen()) {
                it.remove();
            }
        }

        Iterator<Ticket> ticketIterator = tickets.iterator();
        while (ticketIterator.hasNext()) {
            Ticket ticket = ticketIterator.next();
            ticket.updateWithScroll(currentSpeed, deltaTime);
            if (player.intersects(ticket)) {
                ticketsCollected++;
                ticketIterator.remove();
                notifySarcasticEvent("Ticket validé ! +" + Ticket.SCORE_VALUE + " points.");
            } else if (ticket.isOffScreen()) {
                ticketIterator.remove();
            }
        }
    }

    private void startFinale() {
        finaleActive = true;
        hazards.clear();
        tickets.clear();
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
                playerWon = hearts > 0;
                endMessage = playerWon
                        ? "Tu as attrapé le dernier métro ! Bien joué, voyageur."
                        : "Portes fermées. Prochain départ : 05h30.";
                notifyFinaleTriggered(endMessage);
            }
        }
    }

    public void reset() {
        hazards.clear();
        timeRemaining = INITIAL_TIME;
        spawnCooldown = 1.0;
        ticketSpawnCooldown = 3.0;
        distanceMeters = 0.0;
        finaleActive = false;
        gameOver = false;
        metroTrainX = screenWidth + 50;
        metroDoorProgress = 0.0;
        invulnerabilityTimer = 0.0;
        hearts = STARTING_HEARTS;
        ticketsCollected = 0;
        playerWon = false;
        endMessage = "";
        player.reset();
    }

    private void notifySarcasticEvent(String msg) {
        for (GameObserver obs : observers) obs.onSarcasticEvent(msg);
    }

    private void notifyFinaleTriggered(String reason) {
        for (GameObserver obs : observers) obs.onFinaleTriggered(reason);
    }

    public Commuter getPlayer() { return player; }
    public List<Hazard> getHazards() { return hazards; }
    public List<Ticket> getTickets() { return tickets; }
    public double getTimeRemaining() { return timeRemaining; }
    public double getDistanceMeters() { return distanceMeters; }
    public boolean isFinaleActive() { return finaleActive; }
    public boolean isGameOver() { return gameOver; }
    public double getMetroTrainX() { return metroTrainX; }
    public double getMetroDoorProgress() { return metroDoorProgress; }
    public Mode getMode() { return mode; }
    public int getHearts() { return hearts; }
    public boolean isPlayerWon() { return playerWon; }
    public String getEndMessage() { return endMessage; }
    public int getTicketsCollected() { return ticketsCollected; }
    public int getScore() { return (int) distanceMeters + ticketsCollected * Ticket.SCORE_VALUE; }
    public boolean isInvulnerable() { return invulnerabilityTimer > 0.0; }
}
