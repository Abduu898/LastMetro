package com.lastmetro.view;

import com.lastmetro.animation.AnimationClip;
import com.lastmetro.animation.AnimationPlayer;
import com.lastmetro.model.Commuter;
import com.lastmetro.model.GameWorld;
import com.lastmetro.model.Hazard;
import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Draws game screens and placeholder art without owning gameplay or input rules. */
public final class GameRenderer {
    private static final double WIDTH = 1280;
    private static final double HEIGHT = 720;
    private static final double GROUND_Y = 580;
    private static final double CITY_SCROLL_PER_METER = 2.1;
    private static final double[] RUNNING_BOB = {0, -2, -3, -2};
    private static final double[] SLIDING_BOB = {0, 1};

    private final AnimationPlayer playerAnimation = createPlayerAnimation();
    private final java.util.Map<String, SpriteStrip> playerSprites = new java.util.HashMap<>();
    private final java.util.Map<String, SpriteStrip> hazardSprites = new java.util.HashMap<>();
    private SpriteStrip transitCards;
    private SpriteStrip heartSprite;
    private Image parisComposite;
    private Image parisForeground;
    private Image parisStreetBackground;
    private Image part1FarBackground;
    private Image part1MidBuildingsA;
    private Image part1MidBuildingsB;
    private Image part1FrontWalkway;
    private String pixelFontFamily = "Consolas";
    private double menuTime;

    public void loadAssets() {
        var fontResource = GameRenderer.class.getResource("/fonts/PressStart2P-Regular.ttf");
        if (fontResource != null) {
            Font pixelFont = Font.loadFont(fontResource.toExternalForm(), 16);
            if (pixelFont != null) pixelFontFamily = pixelFont.getFamily();
        }
        loadParisComposite();
        loadParisForeground();
        loadParisStreetBackground();
        loadPart1BackgroundLayers();
        String atlas = "/assets/sprites/commuter-atlas.png";
        loadPlayerSprite("RUNNING", atlas,
                new Rectangle2D(65, 34, 181, 208), new Rectangle2D(334, 34, 186, 208),
                new Rectangle2D(631, 34, 177, 208), new Rectangle2D(887, 34, 182, 208));
        loadPlayerSprite("JUMPING", atlas,
                new Rectangle2D(71, 257, 162, 235), new Rectangle2D(355, 257, 169, 235));
        loadPlayerSprite("SLIDING", atlas,
                new Rectangle2D(78, 522, 188, 151), new Rectangle2D(397, 522, 229, 151));
        loadPlayerSprite("CONFUSED", atlas,
                new Rectangle2D(75, 692, 206, 228), new Rectangle2D(355, 692, 209, 228));

        String objects = "/assets/sprites/objects-atlas.png";
        hazardSprites.put("DOG_POOP", loadFrames(objects,
                new Rectangle2D(90, 50, 61, 109), new Rectangle2D(257, 50, 71, 109),
                new Rectangle2D(453, 56, 60, 103)));
        hazardSprites.put("E_SCOOTER", loadFrames(objects,
                new Rectangle2D(65, 225, 214, 133), new Rectangle2D(325, 225, 221, 133)));
        hazardSprites.put("PIGEON", loadFrames(objects,
                new Rectangle2D(91, 423, 134, 119), new Rectangle2D(290, 423, 144, 115),
                new Rectangle2D(533, 453, 114, 85)));
        transitCards = loadFrames(objects,
                new Rectangle2D(63, 614, 187, 115), new Rectangle2D(245, 614, 220, 117));
        heartSprite = loadFrames(objects, new Rectangle2D(355, 842, 51, 47));
    }

    private void loadParisComposite() {
        var resource = GameRenderer.class.getResource("/assets/scenes/paris-composite.jpg");
        if (resource == null) return;
        try {
            Image image = new Image(resource.toExternalForm(), false);
            if (!image.isError() && image.getWidth() >= 2700 && image.getHeight() >= 1500) {
                parisComposite = image;
            }
        } catch (RuntimeException ignored) {
            // Keep the simple background when the optional scene art cannot be loaded.
        }
    }

    private void loadParisForeground() {
        var resource = GameRenderer.class.getResource("/assets/scenes/paris-foreground.png");
        if (resource == null) return;
        Image image = new Image(resource.toExternalForm(), false);
        if (!image.isError() && image.getWidth() >= 2600 && image.getHeight() >= 700) {
            parisForeground = image;
        }
    }

    private void loadParisStreetBackground() {
        var resource = GameRenderer.class.getResource("/assets/scenes/paris-street-background.png");
        if (resource == null) return;
        Image image = new Image(resource.toExternalForm(), false);
        if (!image.isError() && image.getWidth() >= 1200 && image.getHeight() >= 650) {
            parisStreetBackground = image;
        }
    }

    private void loadPart1BackgroundLayers() {
        part1FarBackground = loadOptionalImage("/assets/backgrounds/part1_far_bg.png");
        part1MidBuildingsA = loadOptionalImage("/assets/backgrounds/part1_mid_buildings_A.png");
        part1MidBuildingsB = loadOptionalImage("/assets/backgrounds/part1_mid_buildings_B.png");
        part1FrontWalkway = loadOptionalImage("/assets/backgrounds/part1_front_walkway.png");
    }

    private Image loadOptionalImage(String path) {
        var resource = GameRenderer.class.getResource(path);
        if (resource == null) return null;
        try {
            Image image = new Image(resource.toExternalForm(), false);
            return image.isError() ? null : image;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void loadPlayerSprite(String animation, String path, Rectangle2D... frames) {
        SpriteStrip.loadFrames(GameRenderer.class, path, frames)
                .ifPresent(strip -> playerSprites.put(animation, strip));
    }

    private SpriteStrip loadFrames(String path, Rectangle2D... frames) {
        return SpriteStrip.loadFrames(GameRenderer.class, path, frames).orElse(null);
    }

    public void updateUi(double deltaTime) {
        if (Double.isFinite(deltaTime) && deltaTime > 0.0) menuTime += Math.min(deltaTime, 0.1);
    }

    public void update(GameWorld world, double deltaTime) {
        String state = world.getPlayer().getCurrentState().getStateName();
        if (state.equals("RUNNING") && world.getPlayer().isConfused()) state = "CONFUSED";
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
        drawBackground(gc, screen, world);
        switch (screen) {
            case MENU -> drawMenu(gc);
            case HOW_TO_PLAY -> drawHowToPlay(gc, jumpKey, slideKey);
            case SETTINGS -> drawSettings(gc, jumpKey, slideKey, bindingPrompt);
            case PLAYING, PAUSED, RESULTS -> {
                drawGame(gc, world, bannerText, bannerTimer, endlessHighScore);
                if (screen == GameScreen.PAUSED) {
                    drawPause(gc, world);
                } else if (screen == GameScreen.RESULTS) {
                    drawResults(gc, world, gameOverReason, endlessHighScore);
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
        animation.add("CONFUSED", new AnimationClip(2, 0.18, true));
        return animation;
    }

    private void drawBackground(GraphicsContext gc, GameScreen screen, GameWorld world) {
        gc.setImageSmoothing(false);
        if (part1FarBackground != null && part1MidBuildingsA != null
                && part1MidBuildingsB != null && part1FrontWalkway != null) {
            drawPart1Parallax(gc, world);
            return;
        }
        if (parisStreetBackground != null) {
            gc.drawImage(parisStreetBackground, 0, 0, WIDTH, HEIGHT);
            return;
        }
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        double distance = world == null ? 0 : world.getDistanceMeters();
        double drift = Math.sin(menuTime * 0.22 + distance * 0.004);
        gc.setFill(new LinearGradient(0, 190, 0, HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#88718a")),
                new Stop(0.52, Color.web("#62536d")),
                new Stop(1, Color.web("#403a4b"))));
        gc.fillRect(0, 190, WIDTH, HEIGHT - 190);
        if (parisComposite != null) {
            gc.drawImage(parisComposite, 50, 110, 2652, 486,
                    -20 + drift * 20, 0, WIDTH + 40, 242);
            gc.setFill(new LinearGradient(0, 198, 0, 266, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.TRANSPARENT),
                    new Stop(1, Color.web("#817087"))));
            gc.fillRect(0, 198, WIDTH, 68);
        }
        if (parisForeground != null) {
            drawLoopingCity(gc, world);
        } else if (parisComposite != null) {
            gc.drawImage(parisComposite, 50, 768, 2652, 728,
                    -38 + drift * 38, 210, WIDTH + 76, GROUND_Y - 210);
        }
        drawStreet(gc, world);
    }

    private void drawPart1Parallax(GraphicsContext gc, GameWorld world) {
        double cameraOffset = world == null
                ? menuTime * 31.0
                : world.getDistanceMeters() * 31.0;

        double farHeight = HEIGHT;
        double farWidth = farHeight * part1FarBackground.getWidth()
                / part1FarBackground.getHeight();
        double farOffset = (cameraOffset * 0.10) % farWidth;
        for (double x = -farOffset; x < WIDTH; x += farWidth) {
            gc.drawImage(part1FarBackground, x, 0, farWidth, farHeight);
        }

        double middleHeight = GROUND_Y - 210.0;
        double middleAWidth = middleHeight * part1MidBuildingsA.getWidth()
                / part1MidBuildingsA.getHeight();
        double middleBWidth = middleHeight * part1MidBuildingsB.getWidth()
                / part1MidBuildingsB.getHeight();
        double middleCycleWidth = middleAWidth + middleBWidth;
        double middleOffset = (cameraOffset * 0.50) % middleCycleWidth;
        double middleX = -middleOffset;
        boolean drawA = true;
        while (middleX < WIDTH) {
            Image middleImage = drawA ? part1MidBuildingsA : part1MidBuildingsB;
            double tileWidth = drawA ? middleAWidth : middleBWidth;
            gc.drawImage(middleImage, middleX, GROUND_Y - middleHeight,
                    tileWidth, middleHeight);
            middleX += tileWidth;
            drawA = !drawA;
        }

        double foregroundHeight = HEIGHT;
        double foregroundWidth = foregroundHeight * part1FrontWalkway.getWidth()
                / part1FrontWalkway.getHeight();
        double foregroundOffset = (cameraOffset * 1.00) % foregroundWidth;
        for (double x = -foregroundOffset; x < WIDTH; x += foregroundWidth) {
            gc.drawImage(part1FrontWalkway, x, HEIGHT - foregroundHeight,
                    foregroundWidth, foregroundHeight);
        }
    }

    private void drawLoopingCity(GraphicsContext gc, GameWorld world) {
        double layerY = 210;
        double layerHeight = GROUND_Y - layerY;
        double tileWidth = layerHeight * parisForeground.getWidth() / parisForeground.getHeight();
        double travelled = world == null
                ? menuTime * 24
                : world.getDistanceMeters() * CITY_SCROLL_PER_METER;
        double offset = travelled % tileWidth;
        for (double x = -offset; x < WIDTH; x += tileWidth) {
            gc.drawImage(parisForeground, 0, 0, parisForeground.getWidth(), parisForeground.getHeight(),
                    x, layerY, tileWidth, layerHeight);
        }
    }

    private void drawStreet(GraphicsContext gc, GameWorld world) {
        gc.setFill(Color.web("#433d4c"));
        gc.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);
        gc.setFill(Color.web("#b58b79"));
        gc.fillRect(0, GROUND_Y, WIDTH, 7);
        gc.setFill(Color.web("#775e63"));
        gc.fillRect(0, GROUND_Y + 7, WIDTH, 5);

        double scroll = (world == null ? menuTime * 31 : world.getDistanceMeters() * 31) % 37;
        Color[] stones = {Color.web("#76616a"), Color.web("#695860"), Color.web("#806871"),
                Color.web("#62535d"), Color.web("#725e66"), Color.web("#8a6e72")};
        double[] rowHeight = {12, 14, 16, 18, 21, 24, 27};
        double y = GROUND_Y + 16;
        for (int row = 0; row < rowHeight.length && y < HEIGHT - 36; row++) {
            double height = rowHeight[row];
            double width = 19 + row * 4;
            double start = -width - ((scroll + row * 13) % width);
            int column = 0;
            for (double x = start; x < WIDTH; column++) {
                int variation = Math.floorMod(column * 7 + row * 5, 9);
                double stoneWidth = width - 4 + variation;
                double inset = 2 + ((column * 3 + row) % 3);
                double[] px = {x + inset, x + stoneWidth - inset, x + stoneWidth,
                        x + stoneWidth - 2, x + 2, x};
                double[] py = {y + 1, y + 1 + variation % 3, y + height * 0.46,
                        y + height - 1, y + height - 1, y + height * 0.52};
                gc.setFill(stones[Math.floorMod(column * 5 + row * 3, stones.length)]);
                gc.fillPolygon(px, py, px.length);
                gc.setStroke(Color.rgb(32, 25, 36, 0.24));
                gc.strokePolygon(px, py, px.length);
                x += stoneWidth + 2;
            }
            y += height;
        }

        // A low foreground barrier gives the street depth while leaving the runner readable.
        double railY = HEIGHT - 34;
        gc.setFill(Color.web("#242632"));
        gc.fillRect(0, railY, WIDTH, 7);
        gc.setFill(Color.web("#9b7774"));
        gc.fillRect(0, railY + 7, WIDTH, 3);
        gc.setStroke(Color.web("#292a36"));
        gc.setLineWidth(5);
        for (int x = -18; x < WIDTH; x += 138) {
            gc.strokeLine(x, railY + 10, x + 138, HEIGHT);
            gc.strokeLine(x + 138, railY + 10, x, HEIGHT);
        }
        for (int x = -18; x <= WIDTH + 18; x += 138) {
            gc.setFill(Color.web("#272936"));
            gc.fillRoundRect(x - 7, railY - 10, 14, HEIGHT - railY + 10, 5, 5);
            gc.setFill(Color.web("#c08d75"));
            gc.fillOval(x - 4, railY - 9, 8, 5);
        }
    }

    private void drawMenu(GraphicsContext gc) {
        drawMenuBackdrop(gc);
        gc.setFill(Color.web("#f2cc8f"));
        gc.setFont(Font.font(pixelFontFamily, FontWeight.BOLD, 34));
        gc.fillText("LAST METRO", 440, 190);
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
        gc.setFill(new LinearGradient(0, 0, 0, HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(18, 21, 38, 0.3)),
                new Stop(0.48, Color.rgb(18, 21, 38, 0.22)),
                new Stop(1, Color.rgb(18, 21, 38, 0.68))));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawSettings(GraphicsContext gc, String jumpKey, String slideKey, String bindingPrompt) {
        drawMenuBackdrop(gc);
        gc.setFill(Color.web("#f2cc8f"));
        gc.setFont(Font.font(pixelFontFamily, FontWeight.BOLD, 30));
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
        gc.setFont(Font.font(pixelFontFamily, FontWeight.BOLD, 16));
        gc.fillText(label, 405, y + 42);
    }

    private void drawHowToPlay(GraphicsContext gc, String jumpKey, String slideKey) {
        drawMenuBackdrop(gc);
        gc.setFill(Color.rgb(20, 23, 38, 0.78));
        gc.fillRoundRect(260, 225, 760, 315, 14, 14);
        gc.setStroke(Color.rgb(245, 223, 177, 0.6));
        gc.strokeRoundRect(260, 225, 760, 315, 14, 14);
        gc.setFill(Color.web("#f2cc8f"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 42));
        gc.fillText("HOW TO PLAY", 420, 190);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 23));
        gc.fillText(jumpKey + "     Jump", 370, 280);
        gc.fillText(slideKey + "     Slide", 370, 330);
        gc.fillText("P / ESC              Pause", 370, 380);
        gc.fillText("Avoid hazards. You have three hearts.", 370, 450);
        gc.fillText("Collect Navigo cards for bonus points.", 370, 474);
        gc.fillText("Catch the Metro: reach 500 m before time runs out.", 370, 500);
        gc.fillText("Endless Commute: survive and beat your distance.", 370, 526);
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Consolas", 17));
        gc.fillText("Press ENTER or ESC, or click below", 430, 556);
        drawButton(gc, 470, 580, 340, "BACK TO MENU");
    }

    private void drawGame(GraphicsContext gc, GameWorld world, String bannerText,
                          double bannerTimer, int endlessHighScore) {
        if (world == null) return;
        for (Hazard hazard : world.getHazards()) {
            SpriteStrip sprite = hazardSprites.get(hazard.getType());
            if (sprite != null) {
                int frame = (int) (Math.abs(menuTime * 8.0 + hazard.getX() / 50.0) % sprite.getFrameCount());
                if (hazard.getType().equals("DOG_POOP")) {
                    sprite.drawFitted(gc, frame, hazard.getX() - 2, hazard.getY() - 22,
                            hazard.getWidth() + 4, hazard.getHeight() + 25);
                } else if (hazard.getType().equals("PIGEON")) {
                    sprite.drawFitted(gc, frame, hazard.getX() - 5, hazard.getY() - 4,
                            hazard.getWidth() + 10, hazard.getHeight() + 8);
                } else {
                    sprite.drawFitted(gc, frame, hazard.getX() - 3, hazard.getY() - 4,
                            hazard.getWidth() + 6, hazard.getHeight() + 8);
                }
            } else {
                gc.setFill(switch (hazard.getType()) {
                    case "DOG_POOP" -> Color.SADDLEBROWN;
                    case "E_SCOOTER" -> Color.LIMEGREEN;
                    case "PIGEON" -> Color.LIGHTSLATEGRAY;
                    default -> Color.CRIMSON;
                });
                gc.fillRect(hazard.getX(), hazard.getY(), hazard.getWidth(), hazard.getHeight());
            }
        }
        for (var ticket : world.getTickets()) {
            if (transitCards != null) {
                int frame = (int) (menuTime * 3.0) % transitCards.getFrameCount();
                double pulse = 0.12 + 0.07 * (0.5 + 0.5 * Math.sin(menuTime * 4.0 + ticket.getX() * 0.025));
                gc.setFill(Color.rgb(86, 183, 255, pulse));
                gc.fillOval(ticket.getX() - 10, ticket.getY() - 8, ticket.getWidth() + 20, ticket.getHeight() + 16);
                transitCards.drawFitted(gc, frame, ticket.getX() - 13, ticket.getY() - 4, 50, 38);
            } else {
                gc.setFill(Color.GOLD);
                gc.fillRoundRect(ticket.getX(), ticket.getY(), ticket.getWidth(), ticket.getHeight(), 5, 5);
                gc.setFill(Color.web("#7a4b12"));
                gc.fillText("T", ticket.getX() + 7, ticket.getY() + 20);
            }
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
        SpriteStrip playerSprite = playerSprites.get(playerAnimation.getCurrentName());
        if (playerSprite == null) {
            gc.fillRect(player.getX(), player.getY() + offset, player.getWidth(), player.getHeight());
        } else {
            double spriteHeight = player.getHeight() * 1.55;
            playerSprite.drawFitted(gc, playerAnimation.getCurrentFrame(), player.getX() - 18,
                    player.getY() + offset - (spriteHeight - player.getHeight()),
                    player.getWidth() + 36, spriteHeight);
            if (player.isConfused() || player.isSlowed()) {
                gc.setGlobalAlpha(0.2);
                gc.setFill(player.isConfused() ? Color.YELLOWGREEN : Color.ORANGE);
                gc.fillOval(player.getX() - 8, GROUND_Y - 7, player.getWidth() + 16, 12);
            }
        }
        gc.setGlobalAlpha(1.0);

        drawHudPanel(gc, 20, 16, 310, 60);
        drawHudPanel(gc, 510, 12, 260, 66);
        drawHudPanel(gc, 950, 16, 310, 60);
        gc.setFill(Color.web("#f5dfb1"));
        gc.setFont(Font.font(pixelFontFamily, 9));
        gc.fillText("HEARTS", 34, 31);
        gc.fillText("TICKETS  " + world.getTicketsCollected(), 165, 52);
        for (int heart = 0; heart < GameWorld.STARTING_HEARTS; heart++) {
            double alpha = heart < world.getHearts() ? 1.0 : 0.24;
            if (heartSprite != null) {
                gc.setGlobalAlpha(alpha);
                heartSprite.drawFitted(gc, 0, 34 + heart * 36, 36, 27, 25);
                gc.setGlobalAlpha(1.0);
            } else {
                gc.setFill(heart < world.getHearts() ? Color.CRIMSON : Color.web("#71656c"));
                gc.setFont(Font.font("Serif", 26));
                gc.fillText("♥", 34 + heart * 36, 60);
            }
        }

        gc.setFill(Color.web("#f5dfb1"));
        gc.setFont(Font.font(pixelFontFamily, 9));
        gc.fillText(world.getMode() == GameWorld.Mode.CATCH_THE_METRO ? "LAST METRO" : "ENDLESS RUN", 554, 31);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 27));
        if (world.getMode() == GameWorld.Mode.CATCH_THE_METRO) {
            gc.fillText(String.format("%02d:%02d", (int) world.getTimeRemaining() / 60,
                    (int) world.getTimeRemaining() % 60), 580, 64);
        } else {
            gc.fillText(String.format("%04.0f m", world.getDistanceMeters()), 568, 64);
        }

        gc.setFill(Color.web("#f5dfb1"));
        gc.setFont(Font.font(pixelFontFamily, 9));
        gc.fillText(world.getMode() == GameWorld.Mode.CATCH_THE_METRO ? "DISTANCE" : "PERSONAL BEST", 974, 31);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 25));
        if (world.getMode() == GameWorld.Mode.CATCH_THE_METRO) {
            gc.fillText(String.format("%.0f / %.0f m",
                    Math.min(world.getDistanceMeters(), GameWorld.NORMAL_GOAL_METERS), GameWorld.NORMAL_GOAL_METERS),
                    974, 61);
        } else {
            gc.fillText(String.format("%d m", endlessHighScore), 974, 61);
        }

        if (world.getMode() == GameWorld.Mode.CATCH_THE_METRO) drawProgressBar(gc, world);

        if (bannerTimer > 0 && !bannerText.isEmpty()) {
            gc.setFill(Color.rgb(20, 19, 31, 0.88));
            gc.fillRoundRect(230, 146, 820, 43, 8, 8);
            gc.setStroke(Color.rgb(242, 204, 143, 0.52));
            gc.strokeRoundRect(230, 146, 820, 43, 8, 8);
            gc.setFill(Color.GOLD);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
            gc.fillText(bannerText, 250, 173);
        }
    }

    private void drawHudPanel(GraphicsContext gc, double x, double y, double width, double height) {
        gc.setFill(Color.rgb(20, 23, 38, 0.82));
        gc.fillRoundRect(x + 3, y + 4, width, height, 12, 12);
        gc.setFill(Color.rgb(34, 39, 57, 0.92));
        gc.fillRoundRect(x, y, width, height, 12, 12);
        gc.setStroke(Color.rgb(245, 223, 177, 0.68));
        gc.strokeRoundRect(x, y, width, height, 12, 12);
    }

    private void drawProgressBar(GraphicsContext gc, GameWorld world) {
        double progress = Math.min(1.0, world.getDistanceMeters() / GameWorld.NORMAL_GOAL_METERS);
        double x = 200;
        double width = 880;
        gc.setFill(Color.rgb(20, 23, 38, 0.84));
        gc.fillRoundRect(x, 94, width, 15, 8, 8);
        gc.setFill(Color.web("#f3dba5"));
        gc.fillRoundRect(x + 3, 97, (width - 6) * progress, 9, 6, 6);
        double marker = x + 3 + (width - 6) * progress;
        gc.setFill(Color.WHITE);
        gc.fillRoundRect(marker - 12, 82, 24, 17, 5, 5);
        gc.setFill(Color.web("#39a6cf"));
        gc.fillRect(marker - 9, 87, 18, 5);
        gc.setFill(Color.web("#263144"));
        gc.fillRoundRect(marker - 7, 84, 5, 3, 1, 1);
        gc.fillRoundRect(marker + 2, 84, 5, 3, 1, 1);
        gc.fillOval(marker - 7, 97, 5, 4);
        gc.fillOval(marker + 2, 97, 5, 4);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeLine(x + width - 1, 82, x + width - 1, 111);
        for (int square = 0; square < 3; square++) {
            gc.setFill(square % 2 == 0 ? Color.WHITE : Color.web("#263144"));
            gc.fillRect(x + width - 1 + (square % 2) * 4, 82 + square * 4, 4, 4);
        }
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

    private void drawPause(GraphicsContext gc, GameWorld world) {
        drawOverlayPanel(gc);
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font(pixelFontFamily, FontWeight.BOLD, 28));
        gc.fillText("PAUSED", 530, 260);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 20));
        gc.fillText(String.format("Distance: %.0f m   Score: %d", world.getDistanceMeters(), world.getScore()), 400, 315);
        gc.fillText("P / ESC resumes · M returns to the main menu", 365, 355);
        drawButton(gc, 300, 410, 320, "RESUME");
        drawButton(gc, 660, 410, 320, "MAIN MENU");
    }

    private void drawResults(GraphicsContext gc, GameWorld world, String reason, int endlessHighScore) {
        drawOverlayPanel(gc);
        String title = world.isPlayerWon() ? "YOU CAUGHT THE LAST METRO!" : "RUN OVER";
        gc.setFill(title.startsWith("YOU CAUGHT") ? Color.LIGHTGREEN : Color.GOLD);
        gc.setFont(Font.font(pixelFontFamily, FontWeight.BOLD, 25));
        gc.fillText(title, world.isPlayerWon() ? 250 : 510, 255);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 20));
        gc.fillText(reason, 260, 315);
        gc.fillText(String.format("Distance: %.0f m    Score: %d", world.getDistanceMeters(), world.getScore()), 330, 355);
        if (world.getMode() == GameWorld.Mode.ENDLESS) {
            gc.fillText(String.format("Personal best: %d", endlessHighScore), 500, 390);
        }
        gc.setFont(Font.font("Consolas", 16));
        gc.fillText("R retries · M returns to the main menu", 430, 445);
        drawButton(gc, 300, 485, 320, "TRY AGAIN");
        drawButton(gc, 660, 485, 320, "MAIN MENU");
    }

    private void drawOverlayPanel(GraphicsContext gc) {
        gc.setFill(Color.rgb(10, 10, 20, 0.88));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawButton(GraphicsContext gc, double x, double y, double width, String label) {
        gc.setFill(Color.web("#303552"));
        gc.fillRoundRect(x, y, width, 64, 12, 12);
        gc.setStroke(Color.web("#f2cc8f"));
        gc.strokeRoundRect(x, y, width, 64, 12, 12);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        gc.fillText(label, x + 32, y + 40);
    }

    private void drawTransition(GraphicsContext gc, double alpha) {
        if (alpha <= 0.0) return;
        gc.setFill(Color.rgb(0, 0, 0, alpha));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
    }
}
