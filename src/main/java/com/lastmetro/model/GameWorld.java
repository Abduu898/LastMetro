package com.lastmetro.model;

import com.lastmetro.patterns.factory.HazardFactory;
import com.lastmetro.patterns.factory.HazardFactory.Pattern;
import com.lastmetro.patterns.observer.GameObserver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Core pure-Java simulation holding the commuter, active hazards, countdown, and finale state.
 */
public class GameWorld {
    public static final double INITIAL_TIME = 50.0;
    public static final double BASE_SCROLL_SPEED = 460.0;
    public static final double NORMAL_GOAL_METERS = 500.0;
    public static final int MAX_HEARTS = 3;
    public static final int STARTING_HEARTS = MAX_HEARTS;
    public static final double TRAIN_STOP_X = 680;
    public static final double TRAIN_DOOR_OFFSET = 192;
    private static final double BOARDING_END = 2.10;
    private static final double DOORS_CLOSED = 2.72;
    private static final double DEPARTURE_START = 2.90;

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
    private boolean quickTransfers;
    private double transferDuration;
    private boolean openingPattern = true;
    private Pattern previousPattern;
    private double nextPatternDistance = 1;
    private double nextHeartDistance = 65;
    private int executionScore, cleanDodges, perfectLandings, cleanSections, sectionStartHits;
    private double landingWindow, hazardDodgeNotice;
    private boolean landingAwarded;
    private int heartsCollected, heartBonusPoints, hitsTaken;
    private boolean lastHeartRestored;
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
            double step = Math.min(remaining, 1.0 / 60.0);
            if (routeStage < 2 && !finaleActive) {
                double metresLeft = RouteTimeline.end(routeStage) - distanceMeters;
                step = Math.min(step, metresLeft * 40 / scrollSpeed(step));
            }
            advancePlay(step);
            remaining -= step;
            if (!gameOver && routeStage < 2 && distanceMeters >= RouteTimeline.end(routeStage) - .000001) {
                distanceMeters = RouteTimeline.end(routeStage);
                beginTransfer();
            }
        }
    }

    public void useQuickTransfers() { quickTransfers = true; }

    private void beginTransfer() {
        completeSection();
        transferDuration = quickTransfers ? RouteTimeline.RETRY_TRANSFER_SECONDS : RouteTimeline.TRANSFER_SECONDS;
        transferRemaining = transferDuration;
        hazards.clear();
        coins.clear();
        tickets.clear();
        heartPickups.clear();
        player.releaseInputs();
        openingPattern = true;
        previousPattern = null;
        nextPatternDistance = distanceMeters + 1;
        landingWindow = 0;
        hazardDodgeNotice = 0;
    }

    private double pace() {
        return mode == Mode.ENDLESS
                ? BASE_SCROLL_SPEED * (1.0 + Math.min(distanceMeters / 1000.0, 1.5))
                : BASE_SCROLL_SPEED;
    }

    private double scrollSpeed(double step) {
        double launchEase = .72 + .28 * Math.min(1, (runTime + step) / .55);
        return pace() * launchEase * (player.isSlowed() ? .65 : 1);
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

        double currentSpeed = scrollSpeed(deltaTime);
        runTime += deltaTime;
        currentScrollSpeed = currentSpeed;
        distanceMeters += (currentSpeed * deltaTime) / 40.0;

        boolean wasAirborne = player.getY() + player.getHeight() < groundY - .01;
        player.update(deltaTime);
        landingWindow = Math.max(0, landingWindow - deltaTime);
        hazardDodgeNotice = Math.max(0, hazardDodgeNotice - deltaTime);
        if (wasAirborne && player.getY() + player.getHeight() >= groundY - .01) {
            landingWindow = .22;
            landingAwarded = false;
        }

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

        spawnRoutePattern();

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
                    nextHeartDistance = Math.min(nextHeartDistance, distanceMeters + 12);
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

            if (!h.isTriggered() && h.getX() + h.getWidth() <= player.getX()) {
                h.setTriggered(true);
                cleanDodges++;
                executionScore += 15;
                hazardDodgeNotice = .65;
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
                if (coin.isLandingMarker() && landingWindow > 0 && !landingAwarded
                        && player.getY() + player.getHeight() >= groundY - .01) {
                    landingAwarded = true;
                    perfectLandings++;
                    executionScore += 40;
                    notifySarcasticEvent("PERFECT LANDING  +40");
                }
                coinIterator.remove();
            } else if (coin.getX() < -30) coinIterator.remove();
        }
    }

    private void spawnRoutePattern() {
        if (distanceMeters < nextPatternDistance) return;
        double x = screenWidth + 170;
        double spacing = pace() / BASE_SCROLL_SPEED;
        double sectionEnd = mode == Mode.ENDLESS && routeStage == 2
                ? Double.POSITIVE_INFINITY : RouteTimeline.end(routeStage);
        double approachReserve = routeStage == 2 ? 12 : 6;
        if (distanceMeters + (x + 870 * spacing - player.getX()) / 40 > sectionEnd - approachReserve) return;

        Pattern pattern = hazardFactory.choosePattern(routeStage, openingPattern, previousPattern);
        if (!openingPattern && pattern != Pattern.EMPTY && distanceMeters >= nextHeartDistance)
            pattern = Pattern.RECOVERY;
        openingPattern = false;
        previousPattern = pattern;
        double recovery = routeStage == 0 ? 600 : routeStage == 1 ? 530 : 460;
        double length = switch (pattern) {
            case TOKENS, RECOVERY -> {
                trail(x + 350 * spacing, 67, 4, 54, spacing);
                for (int i = 0; i < 7; i++) {
                    double lift = Math.sin((i + 1) * Math.PI / 8) * 100;
                    coins.add(new Coin(x + (350 + i * 31) * spacing, groundY - 64 - lift));
                }
                if (distanceMeters >= nextHeartDistance) {
                    heartPickups.add(new HeartPickup(x + 443 * spacing, groundY - 148));
                    nextHeartDistance = distanceMeters + 100;
                }
                if (pattern == Pattern.RECOVERY)
                    tickets.add(new Ticket(x + 520 * spacing, groundY - 124));
                yield 650;
            }
            case BARRIER, CRATE, SCOOTER, SKID -> {
                jumpChallenge(x, spacing, pattern);
                yield 300;
            }
            case TRANSFER, PLATFORM -> {
                jumpChallenge(x, spacing, pattern == Pattern.TRANSFER ? Pattern.BARRIER : Pattern.CRATE);
                slideChallenge(x + (pattern == Pattern.TRANSFER ? 590 : 530) * spacing, spacing,
                        pattern == Pattern.PLATFORM);
                yield 710;
            }
            case PIGEON -> {
                slideChallenge(x, spacing, true);
                yield 260;
            }
            case EMPTY -> 0;
        };
        nextPatternDistance = distanceMeters + (length + recovery) * spacing / 40;
    }

    private void jumpChallenge(double x, double spacing, Pattern pattern) {
        double target = x + 155 * spacing;
        hazards.add(switch (pattern) {
            case CRATE -> new StreetObstacle(target, groundY, true);
            case SCOOTER -> new EScooter(target + (target - player.getX()) * .05, groundY);
            case SKID -> new DogPoop(target, groundY);
            default -> new StreetObstacle(target, groundY, false);
        });
        int[] heights = {78, 112, 144, 153, 139, 109, 76};
        for (int i = 0; i < heights.length; i++)
            coins.add(new Coin(x + i * 42 * spacing, groundY - heights[i]));
        for (int offset : new int[] {258, 286, 315}) {
            coins.add(new Coin(x + offset * spacing, groundY - 51, true));
        }
    }

    private void slideChallenge(double target, double spacing, boolean pigeon) {
        hazards.add(pigeon ? new Pigeon(target, groundY) : new OverheadSign(target, groundY));
        trail(target - 66 * spacing, 33, 6, 32, spacing);
    }

    private void trail(double x, int gap, int count, int height, double spacing) {
        for (int i = 0; i < count; i++) coins.add(new Coin(x + i * gap * spacing, groundY - height));
    }

    private void completeSection() {
        if (hitsTaken == sectionStartHits) {
            cleanSections++;
            executionScore += 100;
            notifySarcasticEvent("CLEAN SECTION  +100");
        }
        sectionStartHits = hitsTaken;
    }

    private boolean touchesHazard(Hazard hazard) {
        // Forgive scarf/hair and the last few pixels of shoes at obstacle edges.
        return player.getX() + 6 < hazard.getX() + hazard.getWidth() - 2
                && player.getX() + player.getWidth() - 6 > hazard.getX() + 2
                && player.getY() + 7 < hazard.getY() + hazard.getHeight() - 2
                && player.getY() + player.getHeight() - 3 > hazard.getY() + 2;
    }

    private void startFinale() {
        completeSection();
        finaleActive = true;
        metroTrainX = TRAIN_STOP_X;
        hazards.clear();
        tickets.clear();
        coins.clear();
        heartPickups.clear();
        player.reset();
    }

    private void updateFinale(double deltaTime) {
        finaleTime += deltaTime;
        double doorCenter = TRAIN_STOP_X + TRAIN_DOOR_OFFSET;
        player.setX(Math.min(doorCenter - player.getWidth() / 2, 150 + finaleTime * 365));
        metroDoorProgress = Math.clamp((finaleTime - BOARDING_END) / (DOORS_CLOSED - BOARDING_END), 0, 1);
        double departure = Math.max(0, finaleTime - DEPARTURE_START);
        metroTrainX = TRAIN_STOP_X + 285 * departure * departure;
        if (metroTrainX > screenWidth + 100) {
            gameOver = true;
            playerWon = true;
            endMessage = "Tu as attrapé le dernier métro ! Bien joué, voyageur.";
            notifyFinaleTriggered(endMessage);
        }
    }

    public double getBoardingProgress() {
        return Math.clamp((finaleTime - 1.80) / .30, 0, 1);
    }

    public String getMedal() {
        if (!playerWon) return "";
        if (hitsTaken == 0 && timeRemaining >= 5) return "GOLD";
        if (timeRemaining >= 3) return "SILVER";
        return "BRONZE";
    }

    public double getDodgeNoticeTime() { return hazardDodgeNotice; }
    public int getCleanDodges() { return cleanDodges; }
    public int getPerfectLandings() { return perfectLandings; }
    public int getCleanSections() { return cleanSections; }

    public void reset() {
        hazards.clear();
        tickets.clear(); coins.clear();
        heartPickups.clear(); nextHeartDistance = 65;
        heartsCollected = heartBonusPoints = hitsTaken = 0; lastHeartRestored = false;
        coinsCollected = coinScore = combo = bestCombo = 0;
        comboTimer = finaleTime = landingWindow = hazardDodgeNotice = 0;
        executionScore = cleanDodges = perfectLandings = cleanSections = sectionStartHits = 0;
        landingAwarded = false;
        runTime = magnetTimer = 0; currentScrollSpeed = BASE_SCROLL_SPEED;
        routeStage = 0; transferRemaining = stageStartDistance = 0;
        timeRemaining = INITIAL_TIME;
        quickTransfers = true;
        openingPattern = true;
        previousPattern = null;
        nextPatternDistance = 1;
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
    public int getScore() { return (int) distanceMeters + ticketsCollected * Ticket.SCORE_VALUE + coinScore + heartBonusPoints + executionScore + (playerWon ? 500 : 0); }
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
    public double getTransferProgress() { return isTransferring() ? 1 - transferRemaining / transferDuration : 0; }
    public double getActiveElapsed() { return mode == Mode.CATCH_THE_METRO ? INITIAL_TIME - timeRemaining : runTime; }
    public double getStageProgress() {
        return Math.max(0, Math.min(1, getStageDistance()
                / (RouteTimeline.end(routeStage) - RouteTimeline.start(routeStage))));
    }
    public boolean isInvulnerable() { return invulnerabilityTimer > 0.0; }
}
