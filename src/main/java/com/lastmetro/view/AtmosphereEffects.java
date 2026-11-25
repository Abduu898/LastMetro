package com.lastmetro.view;

import com.lastmetro.feedback.RunFeedback.Event;
import com.lastmetro.model.GameWorld;
import com.lastmetro.model.Ticket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Bounded particles and lighting in logical coordinates. Never changes collision geometry. */
public final class AtmosphereEffects {
    private static final int MAX_PARTICLES = 240;
    private static final Color GOLD = Color.web("#ffdd88");
    private static final Color DUST = Color.web("#d8c4aa");
    private static final Color[] CONFETTI = {GOLD, Color.web("#88dcc5"), Color.web("#ee9390")};
    private static final double GROUND = 576;
    private static final class Particle {
        double x, y, vx, vy, life, duration, size;
        Color color;
        boolean gravity;
    }
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final Random random = new Random(12);
    private double time, impact, squash, pickup, dustClock, celebration;
    private double pickupX, pickupY;
    private String pickupLabel = "+100";
    private Color pickupColor = GOLD;
    private boolean reducedMotion;

    public void setReducedMotion(boolean value) {
        reducedMotion = value;
        if (value) { impact = squash = 0; particles.clear(); }
    }
    public boolean isReducedMotion() { return reducedMotion; }
    public double time() { return time; }
    public double cameraX() { return reducedMotion ? 0 : Math.sin(time * 89) * impact * 17; }
    public double cameraY() { return reducedMotion ? 0 : Math.cos(time * 73) * impact * 10; }
    public double landingScale() { return reducedMotion ? 1 : 1 - squash * .48; }
    public double pickupPulse() { return pickup; }

    public void reset() {
        particles.clear(); time = impact = squash = pickup = dustClock = celebration = 0;
    }

    public void update(GameWorld world, double dt, List<Event> events) {
        advance(dt);
        var player = world.getPlayer();
        double x = player.getX() + player.getWidth() / 2;
        for (Event event : events) switch (event) {
            case LAND -> { emit(x, GROUND - 3, 13, DUST, 100, false); squash = .16; }
            case JUMP -> emit(x, GROUND - 3, 7, DUST, 75, false);
            case HIT -> { emit(x, player.getY() + 35, 24, Color.web("#ffb38c"), 190, true); impact = .24; }
            case TICKET -> {
                pickupColor = GOLD;
                pickupLabel = "+" + Ticket.SCORE_VALUE;
                pickup = .85; pickupX = x; pickupY = player.getY() - 20;
                emit(x, player.getY() + 10, 24, GOLD, 130, false);
            }
            case COIN -> {
                pickupColor = GOLD;
                pickup = .6; pickupX = x; pickupY = player.getY() - 12;
                pickupLabel = "+" + (25 * world.getMultiplier());
                emit(x + 12, player.getY() + 20, 10, GOLD, 105, false);
            }
            case HEART -> {
                pickup=.9;pickupX=x;pickupY=player.getY()-14;pickupColor=Color.web("#f3a296");
                pickupLabel=world.wasLastHeartRestored()?"+1 HEART":"+50";
                emit(x,player.getY()+30,18,pickupColor,110,false);
            }
            case WIN -> { celebration = 2.6; fireworks(); }
            default -> { }
        }
        if (!world.isGameOver() && !world.isFinaleActive() && !world.isTransferring()
                && !player.getCurrentState().getStateName().equals("JUMPING")) {
            dustClock -= dt;
            if (dustClock <= 0) {
                emit(player.getX(), GROUND - 3, 2, DUST, 25, false);
                dustClock = player.isSlideHeld() ? .045 : .13;
            }
        }
    }

    public void updateCelebration(double dt) {
        advance(dt);
        double before = celebration;
        celebration = Math.max(0, celebration - dt);
        if ((int) (before * 2) != (int) (celebration * 2) && celebration > 0) fireworks();
    }

    private void advance(double dt) {
        time += dt;
        impact = Math.max(0, impact - dt); squash = Math.max(0, squash - dt);
        pickup = Math.max(0, pickup - dt);
        for (int i = particles.size() - 1; i >= 0; i--) {
            var p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) { particles.remove(i); continue; }
            p.x += p.vx * dt; p.y += p.vy * dt;
            if (p.gravity) p.vy += 210 * dt;
        }
    }

    private void fireworks() {
        for (int i = 0; i < 3; i++) emit(220 + random.nextInt(840), 155 + random.nextInt(130),
                24, CONFETTI[random.nextInt(CONFETTI.length)], 200, true);
    }

    private void emit(double x, double y, int count, Color color, double speed, boolean gravity) {
        if (reducedMotion) return;
        for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
            var p = new Particle();
            double angle = random.nextDouble() * Math.PI * 2;
            double v = speed * (.3 + random.nextDouble() * .7);
            p.x = x; p.y = y; p.vx = Math.cos(angle) * v - 35; p.vy = Math.sin(angle) * v - 25;
            p.duration = p.life = .35 + random.nextDouble() * .65;
            p.color = color; p.size = 2 + random.nextDouble() * 3; p.gravity = gravity;
            particles.add(p);
        }
    }

    public void drawParticles(GraphicsContext gc) {
        gc.save();
        for (var p : particles) {
            gc.setGlobalAlpha(Math.min(.85, p.life / p.duration)); gc.setFill(p.color);
            gc.fillRect(p.x, p.y, p.size, p.size);
        }
        if (pickup > 0) {
            gc.setGlobalAlpha(Math.min(1, pickup * 3));
            ArcadeHud.label(gc,pickupLabel,pickupX+15,
                    pickupY-(reducedMotion?0:(.9-pickup)*35),21,pickupColor,false);
        }
        gc.restore();
    }

    public void drawLighting(GraphicsContext gc, double camera) {
        gc.save();
        // The foreground artwork repeats with this exact projection; lights follow its lamps.
        double tileWidth = 230.4 * 2752 / (1536.0 - 720);
        double offset = camera % tileWidth;
        for (double origin = -offset; origin < 1280; origin += tileWidth) {
            glow(gc, origin, 434, 72, .25);
            glow(gc, origin + tileWidth * .502, 434, 72, .25);
        }
        // Gentle rim light keeps the warmer scene and cool sky connected.
        gc.setFill(new LinearGradient(0, 300, 0, 600, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(1, Color.rgb(243, 187, 124, .045))));
        gc.fillRect(0, 300, 1280, 300);
        gc.restore();
    }

    private void glow(GraphicsContext gc, double x, double y, double radius, double alpha) {
        gc.setFill(new RadialGradient(0, 0, x, y, radius, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 224, 155, alpha)), new Stop(1, Color.TRANSPARENT)));
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    public void drawAir(GraphicsContext gc) {
        gc.save();
        if (!reducedMotion) {
            // A few drifting leaves, distant dust, and fireflies; deterministic and allocation-light.
            for (int i = 0; i < 22; i++) {
                double x = (i * 137.4 + time * (9 + i % 5)) % 1340 - 30;
                double y = 230 + (i * 47) % 320 + Math.sin(time * .7 + i) * 14;
                gc.setFill(i % 5 == 0 ? Color.rgb(197, 154, 83, .55) : Color.rgb(255, 221, 165, .23));
                gc.fillOval(x, y, i % 5 == 0 ? 6 : 2, 2);
            }
        }
        gc.setFill(new RadialGradient(0, 0, 640, 350, 790, false, CycleMethod.NO_CYCLE,
                new Stop(.45, Color.TRANSPARENT), new Stop(1, Color.rgb(12, 16, 31, .32))));
        gc.fillRect(0, 0, 1280, 720);
        if (!reducedMotion && impact > 0) {
            gc.setFill(Color.rgb(236, 104, 80, impact * .38)); gc.fillRect(0, 0, 1280, 720);
        }
        gc.restore();
    }
}
