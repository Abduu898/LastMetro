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
    public static final int MAX_HEARTS = 3;
    public static final int STARTING_HEARTS = MAX_HEARTS;
    public static final double INITIAL_HAZARD_INTERVAL_SECONDS = 1.65;
    public static final double MIN_HAZARD_INTERVAL_SECONDS = 1.10;
    private static final double INITIAL_TICKET_INTERVAL_SECONDS = 3.0;

    public enum Mode { CATCH_THE_METRO, ENDLESS }

    private final double screenWidth;
    private final double groundY;
    private final Commuter player;
    private final HazardFactory hazardFactory;
    private final Mode mode;
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<Ticket> tickets = new ArrayList<>();
    private final List<Coin> coins = new ArrayList<>();
    private final List<HeartPickup> heartPickups = new ArrayList<>();
    private double heartCooldown = 7;
    private int heartsCollected, heartBonusPoints, hitsTaken;
    private boolean lastHeartRestored;
    private double coinCooldown = .65;
    private int coinsCollected, coinScore, combo, bestCombo;
    private double comboTimer;
    private double finaleTime;
    private double runTime, magnetTimer;
    private double currentScrollSpeed = BASE_SCROLL_SPEED;
    private int routeStage;
    private double transferRemaining;
    private double stageStartDistance;
    private final List<GameObserver> observers = new ArrayList<>();

    private double timeRemaining = INITIAL_TIME;
    private double spawnCooldown = INITIAL_HAZARD_INTERVAL_SECONDS;
    private double ticketSpawnCooldown = INITIAL_TICKET_INTERVAL_SECONDS;
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
        double remaining = deltaTime;
        while (remaining > 0.0000001 && !gameOver) {
            if (transferRemaining > 0) {
                double used = Math.min(remaining, transferRemaining);
                transferRemaining -= used; remaining -= used;
                if (transferRemaining < .0000001) { transferRemaining = 0; routeStage++; stageStartDistance = distanceMeters; }
                continue;
            }
            double untilBoundary = routeStage < 2 && !finaleActive
                    ? RouteTimeline.end(routeStage) - getActiveElapsed() : Double.POSITIVE_INFINITY;
            if (untilBoundary <= .0000001) { beginTransfer(); continue; }
            double step = Math.min(remaining, untilBoundary);
            advancePlay(step); remaining -= step;
            if (routeStage < 2 && !finaleActive && getActiveElapsed() >= RouteTimeline.end(routeStage) - .0000001)
                beginTransfer();
        }
    }

    private void beginTransfer() {
        transferRemaining = RouteTimeline.TRANSFER_SECONDS;
        hazards.clear(); coins.clear(); tickets.clear(); heartPickups.clear(); player.reset();
        heartCooldown = Math.min(heartCooldown, 4);
        spawnCooldown = 1.4; coinCooldown = .7; ticketSpawnCooldown = 2;
        notifySarcasticEvent(routeStage == 0 ? "CORRESPONDANCE · Accès métro" : "CORRESPONDANCE · Vers le quai");
    }

    private void advancePlay(double deltaTime) {
        if (gameOver) return;
        magnetTimer = Math.max(0, magnetTimer - deltaTime);
        comboTimer = Math.max(0, comboTimer - deltaTime);
        if (comboTimer == 0) combo = 0;
        if (invulnerabilityTimer > 0) invulnerabilityTimer = Math.max(0, invulnerabilityTimer - deltaTime);

        // The last train arrives after the route is complete.
        if (finaleActive) {
            updateFinale(deltaTime);
            return;
        }

        if (mode == Mode.CATCH_THE_METRO) {
            if (distanceMeters >= NORMAL_GOAL_METERS) {
                startFinale();
                return;
            }
            deltaTime = Math.min(deltaTime, timeRemaining);
            timeRemaining = Math.max(0.0, timeRemaining - deltaTime);
            if (timeRemaining < .0000001) timeRemaining = 0;
        }

        double rampedSpeed = mode == Mode.ENDLESS
                ? BASE_SCROLL_SPEED * (1.0 + Math.min(distanceMeters / 1000.0, 1.5))
                : BASE_SCROLL_SPEED;
        runTime += deltaTime;
        double launchEase = .72 + .28 * Math.min(1, runTime / .55);
        double currentSpeed = rampedSpeed * launchEase * (player.isSlowed() ? .65 : 1);
        currentScrollSpeed = currentSpeed;
        distanceMeters += (currentSpeed * deltaTime) / 40.0;

        player.update(deltaTime);

        // Resolve the finish using this step's movement, including the last fraction of a second.
        if (mode == Mode.CATCH_THE_METRO && distanceMeters >= NORMAL_GOAL_METERS) {
            distanceMeters = NORMAL_GOAL_METERS;
            startFinale();
            return;
        }
        if (mode == Mode.CATCH_THE_METRO && timeRemaining <= 0) {
            gameOver = true;
            endMessage = "The last métro left before you reached the platform.";
            notifyFinaleTriggered(endMessage);
            return;
        }

        // Tighten obstacle spacing gradually as each mode progresses.
        spawnCooldown -= deltaTime;
        boolean safeTransferApproach = routeStage < 2 && RouteTimeline.end(routeStage) - getActiveElapsed() < 2.8;
        if (spawnCooldown <= 0 && !safeTransferApproach && (mode == Mode.ENDLESS || getRouteProgress() < .88)) {
            hazards.add(hazardFactory.createRandomHazard(screenWidth + 60, groundY));
            spawnCooldown = hazardSpawnInterval(mode, timeRemaining, distanceMeters);
        }

        // Arcs sit within a normal jump, with flat trails between challenges.
        coinCooldown -= deltaTime;
        if (coinCooldown <= 0 && (mode == Mode.ENDLESS || getRouteProgress() < .96)) {
            boolean arc = ((int) (distanceMeters / 20) & 1) == 0;
            for (int i = 0; i < 7; i++) coins.add(new Coin(screenWidth + 70 + i * 45,
                    groundY - 54 - (arc ? Math.sin(i * Math.PI / 6) * 106 : 0)));
            coinCooldown = 2.35;
        }

        ticketSpawnCooldown -= deltaTime;
        if (ticketSpawnCooldown <= 0.0) {
            tickets.add(new Ticket(screenWidth + 60, groundY - Commuter.STANDING_HEIGHT - 45));
            ticketSpawnCooldown = INITIAL_TICKET_INTERVAL_SECONDS;
        }

        heartCooldown -= deltaTime;
        if (heartCooldown <= 0 && !safeTransferApproach && (mode == Mode.ENDLESS || getRouteProgress() < .9)) {
            heartPickups.add(new HeartPickup(screenWidth + 60, groundY - 54));
            heartCooldown = 10;
        }

        // Move hazards & check AABB collisions
        Iterator<Hazard> it = hazards.iterator();
        while (it.hasNext()) {
            Hazard h = it.next();
            h.updateWithScroll(currentSpeed, deltaTime);

            if (!h.isTriggered() && touchesHazard(h)) {
                h.setTriggered(true);
                if (invulnerabilityTimer <= 0.0) {
                    hearts = Math.max(0, hearts - 1);
                    hitsTaken++;
                    heartCooldown = Math.min(heartCooldown, 2);
                    combo = 0; comboTimer = 0;
                    invulnerabilityTimer = 1.0;
                    String roast = h.onCollide(player);
                    notifySarcasticEvent(roast + "  HEARTS: " + hearts + "/" + MAX_HEARTS);
                    if (hearts == 0) {
                        gameOver = true;
                        endMessage = "Out of hearts. Catch your breath and try again.";
                        notifyFinaleTriggered(endMessage);
                        return;
                    }
                }
            }

            if (h.isOffScreen()) {
                it.remove();
            }
        }

        var heartIterator = heartPickups.iterator();
        while (heartIterator.hasNext()) {
            var heart = heartIterator.next(); heart.scroll(currentSpeed, deltaTime);
            if (player.intersects(heart)) {
                heartsCollected++;
                lastHeartRestored = hearts < MAX_HEARTS;
                if (lastHeartRestored) hearts++;
                else heartBonusPoints += HeartPickup.FULL_HEALTH_BONUS;
                heartIterator.remove();
                notifySarcasticEvent(lastHeartRestored ? "HEART RESTORED · " + hearts + "/" + MAX_HEARTS
                        : "FULL HEALTH · +" + HeartPickup.FULL_HEALTH_BONUS + " points");
            } else if (heart.getX() + heart.getWidth() < 0) heartIterator.remove();
        }

        Iterator<Ticket> ticketIterator = tickets.iterator();
        while (ticketIterator.hasNext()) {
            Ticket ticket = ticketIterator.next();
            ticket.updateWithScroll(currentSpeed, deltaTime);
            if (player.intersects(ticket)) {
                ticketsCollected++;
                magnetTimer = 5;
                ticketIterator.remove();
                notifySarcasticEvent("NAVIGO MAGNET · 5 seconds · +" + Ticket.SCORE_VALUE + " points");
            } else if (ticket.isOffScreen()) {
                ticketIterator.remove();
            }
        }

        var coinIterator = coins.iterator();
        while (coinIterator.hasNext()) {
            var coin = coinIterator.next(); coin.scroll(currentSpeed, deltaTime);
            double dx = player.getX() + player.getWidth() / 2 - coin.getX() - 12;
            double dy = player.getY() + player.getHeight() / 2 - coin.getY() - 14;
            if (magnetTimer > 0 && Math.abs(dx) < 230 && Math.abs(dy) < 180) {
                double pull = 1 - Math.exp(-deltaTime * 14);
                coin.setX(coin.getX() + dx * pull); coin.setY(coin.getY() + dy * pull);
            }
            if (player.intersects(coin)) {
                coinsCollected++; combo++; comboTimer = 2.8;
                bestCombo = Math.max(bestCombo, combo);
                coinScore += Coin.VALUE * getMultiplier();
                coinIterator.remove();
            } else if (coin.getX() < -30) coinIterator.remove();
        }
    }

    static double hazardSpawnInterval(Mode mode, double timeRemaining, double distanceMeters) {
        double urgency = mode == Mode.ENDLESS
                ? distanceMeters / 1000.0
                : 1.0 - (timeRemaining / INITIAL_TIME);
        urgency = Math.max(0.0, Math.min(1.0, urgency));
        return INITIAL_HAZARD_INTERVAL_SECONDS
                - urgency * (INITIAL_HAZARD_INTERVAL_SECONDS - MIN_HAZARD_INTERVAL_SECONDS);
    }

    private boolean touchesHazard(Hazard hazard) {
        // Forgive scarf/hair and the last few pixels of shoes at obstacle edges.
        return player.getX() + 6 < hazard.getX() + hazard.getWidth() - 2
                && player.getX() + player.getWidth() - 6 > hazard.getX() + 2
                && player.getY() + 7 < hazard.getY() + hazard.getHeight() - 2
                && player.getY() + player.getHeight() - 3 > hazard.getY() + 2;
    }

    private void startFinale() {
        finaleActive = true;
        hazards.clear();
        tickets.clear();
        coins.clear();
        heartPickups.clear();
        player.releaseInputs();
        notifySarcasticEvent("CHÂTELET · The last train is waiting. You made it!");
    }

    private void updateFinale(double deltaTime) {
        finaleTime += deltaTime;
        // A short, readable boarding beat before the victory screen.
        double targetTrainX = 760.0;
        if (finaleTime < 1.7) {
            metroTrainX = Math.max(targetTrainX, metroTrainX - 520.0 * deltaTime);
            player.setX(Math.min(660.0, player.getX() + 340.0 * deltaTime));
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
        tickets.clear(); coins.clear();
        heartPickups.clear(); heartCooldown = 7;
        heartsCollected = heartBonusPoints = hitsTaken = 0; lastHeartRestored = false;
        coinsCollected = coinScore = combo = bestCombo = 0;
        comboTimer = finaleTime = 0; coinCooldown = .65;
        runTime = magnetTimer = 0; currentScrollSpeed = BASE_SCROLL_SPEED;
        routeStage = 0; transferRemaining = stageStartDistance = 0;
        timeRemaining = INITIAL_TIME;
        spawnCooldown = INITIAL_HAZARD_INTERVAL_SECONDS;
        ticketSpawnCooldown = INITIAL_TICKET_INTERVAL_SECONDS;
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
    public int getScore() { return (int) distanceMeters + ticketsCollected * Ticket.SCORE_VALUE + coinScore + heartBonusPoints + (playerWon ? 500 : 0); }
    public List<HeartPickup> getHeartPickups() { return heartPickups; }
    public int getHeartsCollected() { return heartsCollected; }
    public int getHitsTaken() { return hitsTaken; }
    public boolean wasLastHeartRestored() { return lastHeartRestored; }
    public List<Coin> getCoins() { return coins; }
    public int getCoinsCollected() { return coinsCollected; }
    public int getCombo() { return combo; }
    public int getBestCombo() { return bestCombo; }
    public int getMultiplier() { return 1 + Math.min(3, combo / 7); }
    public double getComboProgress() { return comboTimer / 2.8; }
    public double getRouteProgress() { return Math.min(1, distanceMeters / NORMAL_GOAL_METERS); }
    public double getFinaleTime() { return finaleTime; }
    public double getMagnetTime() { return magnetTimer; }
    public double getCurrentScrollSpeed() { return currentScrollSpeed; }
    public int getRouteStage() { return routeStage; }
    public double getStageDistance() { return Math.max(0, distanceMeters - stageStartDistance); }
    public boolean isTransferring() { return transferRemaining > 0; }
    public double getTransferProgress() { return isTransferring() ? 1 - transferRemaining / RouteTimeline.TRANSFER_SECONDS : 0; }
    public double getActiveElapsed() { return mode == Mode.CATCH_THE_METRO ? INITIAL_TIME - timeRemaining : runTime; }
    public double getStageProgress() {
        return Math.max(0, Math.min(1, (getActiveElapsed() - RouteTimeline.start(routeStage))
                / (RouteTimeline.end(routeStage) - RouteTimeline.start(routeStage))));
    }
    public boolean isInvulnerable() { return invulnerabilityTimer > 0.0; }
}
