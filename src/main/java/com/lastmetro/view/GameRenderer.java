package com.lastmetro.view;

import com.lastmetro.animation.AnimationClip;
import com.lastmetro.animation.AnimationPlayer;
import com.lastmetro.model.Commuter;
import com.lastmetro.model.GameWorld;
import com.lastmetro.model.Hazard;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Draws game screens and placeholder art without owning gameplay or input rules. */
public final class GameRenderer {
    private static final double WIDTH = 1280;
    private static final double HEIGHT = 720;
    private static final double GROUND_Y = 580;
    private static final double[] RUNNING_BOB = {0, -2, -3, -2};
    private static final double[] SLIDING_BOB = {0, 1};

    private final AnimationPlayer playerAnimation = createPlayerAnimation();
    private double menuTime;

    public void updateUi(double deltaTime) {
        if (Double.isFinite(deltaTime) && deltaTime > 0.0) menuTime += Math.min(deltaTime, 0.1);
    }

    public void update(GameWorld world, double deltaTime) {
        String state = world.getPlayer().getCurrentState().getStateName();
        if (!state.equals(playerAnimation.getCurrentName())) playerAnimation.play(state);
        playerAnimation.update(deltaTime);
    }

    public void resetAnimations() {
        playerAnimation.play("RUNNING");
        playerAnimation.restart();
    }

    public void render(GraphicsContext gc, GameScreen screen, GameWorld world,
                       String bannerText, double bannerTimer, String gameOverReason,
                       int endlessHighScore, String jumpKey, String slideKey,
                       String bindingPrompt, double transitionAlpha) {
        drawBackground(gc, screen);
        switch (screen) {
            case MENU -> drawMenu(gc);
            case HOW_TO_PLAY -> drawHowToPlay(gc, jumpKey, slideKey);
            case SETTINGS -> drawSettings(gc, jumpKey, slideKey, bindingPrompt);
            case PLAYING, PAUSED, RESULTS -> {
                drawGame(gc, world, bannerText, bannerTimer, endlessHighScore);
                if (screen == GameScreen.PAUSED) {
                    drawOverlay(gc, "PAUSED", "P / ESC  Resume     M  Main menu");
                } else if (screen == GameScreen.RESULTS) {
                    String title = world.isPlayerWon() ? "YOU CAUGHT THE LAST METRO!" : "RUN OVER";
                    String results = gameOverReason + String.format("\nDistance: %.0f m   Score: %d",
                            world.getDistanceMeters(), world.getScore());
                    if (world.getMode() == GameWorld.Mode.ENDLESS) {
                        results += String.format("   Best: %d", endlessHighScore);
                    }
                    drawOverlay(gc, title, results + "\n\nR  Try again     M  Main menu");
                }
            }
        }
        drawTransition(gc, transitionAlpha);
    }

    private static AnimationPlayer createPlayerAnimation() {
        AnimationPlayer animation = new AnimationPlayer();
        animation.add("RUNNING", new AnimationClip(4, 0.12, true));
        animation.add("JUMPING", new AnimationClip(2, 0.14, false));
        animation.add("SLIDING", new AnimationClip(2, 0.18, true));
        return animation;
    }

    private void drawBackground(GraphicsContext gc, GameScreen screen) {
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        if (screen == GameScreen.MENU || screen == GameScreen.HOW_TO_PLAY) return;
        gc.setFill(Color.web("#4a4e69"));
        gc.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);
    }

    private void drawMenu(GraphicsContext gc) {
        drawMenuBackdrop(gc);
        gc.setFill(Color.web("#f2cc8f"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 52));
        gc.fillText("DERNIÈRE RAME", 390, 190);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 22));
        gc.fillText("A Parisian sprint for the last métro", 390, 235);
        drawMenuButton(gc, 280, "1  CATCH THE LAST METRO");
        drawMenuButton(gc, 360, "2  ENDLESS COMMUTE");
        drawMenuButton(gc, 440, "H  HOW TO PLAY");
        drawMenuButton(gc, 520, "O  CONTROLS & SETTINGS");
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Consolas", 16));
        gc.fillText("Choose with the number keys or click an option", 390, 630);
    }

    private void drawMenuBackdrop(GraphicsContext gc) {
        gc.setFill(Color.web("#242942"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        gc.setFill(Color.web("#303552"));
        for (int i = 0; i < 12; i++) {
            double x = (i * 137 - menuTime * (9 + i % 3 * 3)) % (WIDTH + 120);
            if (x < -120) x += WIDTH + 120;
            double height = 54 + (i * 29) % 94;
            gc.fillRect(x, 610 - height, 84, height);
            gc.setFill(Color.web("#f2cc8f", 0.55));
            gc.fillRect(x + 13, 624 - height, 4, 5);
            gc.fillRect(x + 33, 624 - height, 4, 5);
            gc.fillRect(x + 53, 624 - height, 4, 5);
            gc.setFill(Color.web("#303552"));
        }
        gc.setFill(Color.web("#f2cc8f"));
        gc.fillRect(0, 610, WIDTH, 2);
        double metroX = (menuTime * 85) % (WIDTH + 190) - 120;
        gc.setFill(Color.web("#d7e4e8"));
        gc.fillRoundRect(metroX, 586, 76, 22, 5, 5);
        gc.setFill(Color.web("#00b4d8"));
        gc.fillRect(metroX, 600, 76, 4);
        gc.setFill(Color.web("#f2cc8f"));
    }

    private void drawSettings(GraphicsContext gc, String jumpKey, String slideKey, String bindingPrompt) {
        drawMenuBackdrop(gc);
        gc.setFill(Color.web("#f2cc8f"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 42));
        gc.fillText("CONTROLS & SETTINGS", 300, 180);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 22));
        gc.setFill(Color.web("#303552"));
        gc.fillRoundRect(360, 250, 560, 80, 10, 10);
        gc.fillRoundRect(360, 360, 560, 80, 10, 10);
        gc.setStroke(Color.web("#f2cc8f"));
        gc.strokeRoundRect(360, 250, 560, 80, 10, 10);
        gc.strokeRoundRect(360, 360, 560, 80, 10, 10);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 22));
        gc.fillText("Jump   " + (bindingPrompt.equals("jump") ? "Press a key..." : jumpKey), 420, 295);
        gc.fillText("Slide    " + (bindingPrompt.equals("slide") ? "Press a key..." : slideKey), 420, 405);
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Consolas", 17));
        gc.fillText("Click a row, then press its new key. J / S also select a row.", 315, 510);
        String note = bindingPrompt.equals("reserved") ? "That key is reserved. Choose a different one."
                : bindingPrompt.equals("unsaved") ? "Could not save settings. Check folder permissions."
                : "P / ESC / ENTER stay reserved. ENTER / ESC returns.";
        gc.fillText(note, 335, 545);
    }

    private void drawMenuButton(GraphicsContext gc, double y, String label) {
        gc.setFill(Color.web("#303552"));
        gc.fillRoundRect(360, y, 560, 66, 12, 12);
        gc.setStroke(Color.web("#f2cc8f"));
        gc.strokeRoundRect(360, y, 560, 66, 12, 12);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 22));
        gc.fillText(label, 405, y + 42);
    }

    private void drawHowToPlay(GraphicsContext gc, String jumpKey, String slideKey) {
        gc.setFill(Color.web("#f2cc8f"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 42));
        gc.fillText("HOW TO PLAY", 420, 190);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 23));
        gc.fillText(jumpKey + "     Jump", 370, 280);
        gc.fillText(slideKey + "     Slide", 370, 330);
        gc.fillText("P / ESC              Pause", 370, 380);
        gc.fillText("Avoid hazards. You have three hearts.", 370, 450);
        gc.fillText("Catch the Metro: reach 500 m before time runs out.", 370, 490);
        gc.fillText("Endless Commute: survive and beat your distance.", 370, 530);
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Consolas", 17));
        gc.fillText("Press ENTER or ESC to return", 430, 610);
    }

    private void drawGame(GraphicsContext gc, GameWorld world, String bannerText,
                          double bannerTimer, int endlessHighScore) {
        if (world == null) return;
        for (Hazard hazard : world.getHazards()) {
            switch (hazard.getType()) {
                case "DOG_POOP" -> gc.setFill(Color.SADDLEBROWN);
                case "E_SCOOTER" -> gc.setFill(Color.LIMEGREEN);
                case "PIGEON" -> gc.setFill(Color.LIGHTSLATEGRAY);
                default -> gc.setFill(Color.CRIMSON);
            }
            gc.fillRect(hazard.getX(), hazard.getY(), hazard.getWidth(), hazard.getHeight());
        }
        for (var ticket : world.getTickets()) {
            gc.setFill(Color.GOLD);
            gc.fillRoundRect(ticket.getX(), ticket.getY(), ticket.getWidth(), ticket.getHeight(), 5, 5);
            gc.setFill(Color.web("#7a4b12"));
            gc.fillText("T", ticket.getX() + 7, ticket.getY() + 20);
        }

        if (world.isFinaleActive()) drawMetro(gc, world);

        Commuter player = world.getPlayer();
        gc.setFill(player.isConfused() ? Color.YELLOWGREEN : player.isSlowed() ? Color.ORANGE : Color.DODGERBLUE);
        double offset = switch (playerAnimation.getCurrentName()) {
            case "RUNNING" -> RUNNING_BOB[playerAnimation.getCurrentFrame()];
            case "SLIDING" -> SLIDING_BOB[playerAnimation.getCurrentFrame()];
            default -> 0;
        };
        if (world.isInvulnerable() && (System.nanoTime() / 100_000_000L) % 2 == 0) gc.setGlobalAlpha(0.35);
        gc.fillRect(player.getX(), player.getY() + offset, player.getWidth(), player.getHeight());
        gc.setGlobalAlpha(1.0);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
        if (world.getMode() == GameWorld.Mode.CATCH_THE_METRO) {
            gc.fillText(String.format("TIME: %05.2f", world.getTimeRemaining()), 25, 42);
            gc.fillText(String.format("ROUTE: %.0f / %.0f m",
                    Math.min(world.getDistanceMeters(), GameWorld.NORMAL_GOAL_METERS), GameWorld.NORMAL_GOAL_METERS), 340, 42);
            drawProgressBar(gc, world);
        } else {
            gc.fillText("ENDLESS", 25, 42);
            gc.fillText(String.format("DISTANCE: %.0f m", world.getDistanceMeters()), WIDTH - 330, 42);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
            gc.fillText(String.format("BEST: %d", endlessHighScore), WIDTH - 330, 76);
        }
        gc.setFill(Color.CRIMSON);
        gc.fillText("♥ ".repeat(world.getHearts()), 25, 82);
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        gc.fillText("TICKETS: " + world.getTicketsCollected(), 170, 78);

        if (bannerTimer > 0 && !bannerText.isEmpty()) {
            gc.setFill(Color.rgb(0, 0, 0, 0.75));
            gc.fillRoundRect(240, 100, 800, 48, 8, 8);
            gc.setFill(Color.GOLD);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 17));
            gc.fillText(bannerText, 260, 130);
        }
    }

    private void drawProgressBar(GraphicsContext gc, GameWorld world) {
        double progress = Math.min(1.0, world.getDistanceMeters() / GameWorld.NORMAL_GOAL_METERS);
        gc.setFill(Color.web("#303552"));
        gc.fillRoundRect(25, 102, 1230, 18, 8, 8);
        gc.setFill(Color.web("#f2cc8f"));
        gc.fillRoundRect(25, 102, 1230 * progress, 18, 8, 8);
    }

    private void drawMetro(GraphicsContext gc, GameWorld world) {
        double trainX = world.getMetroTrainX();
        gc.setFill(Color.web("#e0e1dd"));
        gc.fillRect(trainX, GROUND_Y - 160, 480, 160);
        gc.setFill(Color.web("#00b4d8"));
        gc.fillRect(trainX, GROUND_Y - 70, 480, 22);
        double doorWidth = 45 * world.getMetroDoorProgress();
        gc.setFill(Color.DARKSLATEGRAY);
        gc.fillRect(trainX + 40, GROUND_Y - 135, doorWidth, 130);
        gc.fillRect(trainX + 130 - doorWidth, GROUND_Y - 135, doorWidth, 130);
    }

    private void drawOverlay(GraphicsContext gc, String title, String message) {
        gc.setFill(Color.rgb(10, 10, 20, 0.88));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        gc.setFill(title.startsWith("YOU CAUGHT") ? Color.LIGHTGREEN : Color.GOLD);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 38));
        gc.fillText(title, 250, 285);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 20));
        String[] lines = message.split("\\n");
        for (int i = 0; i < lines.length; i++) gc.fillText(lines[i], 260, 350 + i * 34);
    }

    private void drawTransition(GraphicsContext gc, double alpha) {
        if (alpha <= 0.0) return;
        gc.setFill(Color.rgb(0, 0, 0, alpha));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
    }
}
