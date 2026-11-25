package com.lastmetro.view;

import com.lastmetro.animation.AnimationClip;
import com.lastmetro.animation.AnimationPlayer;
import com.lastmetro.model.Commuter;
import com.lastmetro.model.GameWorld;
import com.lastmetro.model.Hazard;
import com.lastmetro.feedback.RunFeedback;
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
    private static final double GROUND_Y = 576;
    private static final double CITY_SCROLL_PER_METER = 2.1;
    private static final double[] SLIDING_BOB = {0, 1};

    private AnimationPlayer playerAnimation = createPlayerAnimation();
    private boolean pixelRunner;
    private final java.util.Map<String, SpriteStrip> playerSprites = new java.util.HashMap<>();
    private final java.util.Map<String, SpriteStrip> hazardSprites = new java.util.HashMap<>();
    private SpriteStrip transitCards;
    private Image parisComposite;
    private Image parisForeground;
    private Image parisStreetBackground;
    private Image part1FarBackground;
    private Image part1MidBuildingsA;
    private Image part1MidBuildingsB;
    private Image part1FrontWalkway;
    private String pixelFontFamily = "Consolas";
    private double menuTime;
    private final AtmosphereEffects effects = new AtmosphereEffects();
    private final RunFeedback feedback = new RunFeedback();
    private boolean soundEnabled = true;
    private boolean cameraMotion;
    private final ArcadeArt arcade = new ArcadeArt();
    private final ArcadeHud arcadeHud = new ArcadeHud(arcade);
    private final PixelRouteScenery pixelRoute = new PixelRouteScenery();
    private double pointerX = -1, pointerY = -1;
    private String jumpLabel = "SPACE", slideLabel = "DOWN";
    public void setPointer(double x, double y) { pointerX = x; pointerY = y; }

    public void setPresentationSettings(boolean sound, boolean reducedMotion) {
        soundEnabled = sound;
        effects.setReducedMotion(reducedMotion);
    }

    public void loadAssets() {
        pixelRoute.load(); arcadeHud.load();
        var fontResource = GameRenderer.class.getResource("/fonts/PressStart2P-Regular.ttf");
        if (fontResource != null) {
            Font pixelFont = Font.loadFont(fontResource.toExternalForm(), 16);
            if (pixelFont != null) pixelFontFamily = pixelFont.getFamily();
        }
        loadParisComposite();
        loadParisForeground();
        loadParisStreetBackground();
        loadPart1BackgroundLayers();
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
        String runner = "/assets/pixel-route/commuter.png";
        // Measured alpha bounds: generation is not assumed to produce exact grid baselines.
        var run = SpriteStrip.loadFrames(GameRenderer.class, runner,
                new Rectangle2D(89, 47, 253, 369), new Rectangle2D(542, 45, 231, 371),
                new Rectangle2D(948, 44, 311, 350), new Rectangle2D(1435, 48, 210, 368),
                new Rectangle2D(101, 493, 225, 366), new Rectangle2D(561, 495, 205, 364));
        var jump = SpriteStrip.loadFrames(GameRenderer.class, runner, new Rectangle2D(975, 472, 254, 293));
        var slide = SpriteStrip.loadFrames(GameRenderer.class, runner, new Rectangle2D(1347, 576, 388, 282));
        if (run.isPresent() && jump.isPresent() && slide.isPresent()) {
            pixelRunner = true;
            playerSprites.put("RUNNING", run.get()); playerSprites.put("CONFUSED", run.get());
            playerSprites.put("JUMPING", jump.get()); playerSprites.put("SLIDING", slide.get());
            playerAnimation = new AnimationPlayer();
            playerAnimation.add("RUNNING", new AnimationClip(6, .075, true));
            playerAnimation.add("CONFUSED", new AnimationClip(6, .085, true));
            playerAnimation.add("JUMPING", new AnimationClip(1, .14, false));
            playerAnimation.add("SLIDING", new AnimationClip(1, .18, true));
        }
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

    private SpriteStrip loadFrames(String path, Rectangle2D... frames) {
        return SpriteStrip.loadFrames(GameRenderer.class, path, frames).orElse(null);
    }

    public void updateUi(double deltaTime) {
        updateUi(deltaTime, false);
    }

    public void updateUi(double deltaTime, boolean results) {
        if (Double.isFinite(deltaTime) && deltaTime > 0.0) menuTime += Math.min(deltaTime, 0.1);
        if (results && Double.isFinite(deltaTime) && deltaTime > 0) effects.updateCelebration(Math.min(deltaTime, .1));
    }

    public java.util.List<RunFeedback.Event> update(GameWorld world, double deltaTime) {
        String state = world.getPlayer().getCurrentState().getStateName();
        if (state.equals("RUNNING") && world.getPlayer().isConfused()) state = "CONFUSED";
        if (!state.equals(playerAnimation.getCurrentName())) playerAnimation.play(state);
        double animationSpeed = Math.max(.65, world.getCurrentScrollSpeed() / GameWorld.BASE_SCROLL_SPEED);
        playerAnimation.update(deltaTime * animationSpeed);
        var events = feedback.update(world, deltaTime);
        effects.update(world, deltaTime, events);
        return events;
    }

    public void resetAnimations() {
        feedback.reset(); effects.reset();
        playerAnimation.play("RUNNING");
        playerAnimation.restart();
    }

    public void render(GraphicsContext gc, GameScreen screen, GameWorld world,
                       String bannerText, double bannerTimer, String gameOverReason,
                       int endlessHighScore, String jumpKey, String slideKey,
                       String bindingPrompt, double transitionAlpha) {
        jumpLabel = jumpKey; slideLabel = slideKey;
        cameraMotion = screen == GameScreen.PLAYING;
        gc.save();
        if (screen == GameScreen.PLAYING) gc.translate(effects.cameraX(), effects.cameraY());
        drawBackground(gc, screen, world);
        gc.restore();
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
                    if (world.isPlayerWon()) effects.drawParticles(gc);
                }
            }
        }
        if (screen == GameScreen.PLAYING && world != null) pixelRoute.transfer(gc, world, pixelFontFamily);
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
        if (pixelRoute.isReady()) {
            boolean gameplay = screen == GameScreen.PLAYING || screen == GameScreen.PAUSED || screen == GameScreen.RESULTS;
            boolean nextSection = world != null && world.isTransferring() && world.getTransferProgress() >= .5;
            if (gameplay && world != null && (world.getRouteStage() > 0 || nextSection)) {
                pixelRoute.continuation(gc, world, menuTime); return;
            }
        }
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
                : world.getDistanceMeters() * 40.0;

        double farHeight = HEIGHT;
        double farWidth = farHeight * part1FarBackground.getWidth()
                / part1FarBackground.getHeight();
        double farOffset = (cameraOffset * 0.10) % farWidth;
        for (double x = -farOffset; x < WIDTH; x += farWidth) {
            gc.drawImage(part1FarBackground, x, 0, farWidth, farHeight);
        }

        double middleHeight = HEIGHT * 0.70;
        double middleAWidth = middleHeight * part1MidBuildingsA.getWidth()
                / part1MidBuildingsA.getHeight();
        long tileWidth = Math.round(middleAWidth);
        double middleCycleWidth = tileWidth * 2;
        double middleOffset = (cameraOffset * 0.35) % middleCycleWidth;
        double midScrollX = -middleOffset;
        double x0 = Math.floor(midScrollX);
        long overlap = 1;

        int tileIndex = 0;
        for (int i = 0; ; i++) {
            double x = x0 + i * tileWidth;
            if (x >= WIDTH) break;
            Image middleImage = tileIndex % 2 == 0
                    ? part1MidBuildingsA
                    : part1MidBuildingsB;
            gc.drawImage(middleImage, x, HEIGHT * 0.70 - middleHeight,
                    tileWidth + overlap, middleHeight);
            tileIndex++;
        }

        double walkwayTop = HEIGHT * 0.68;
        double walkwayHeight = HEIGHT - walkwayTop;
        double walkwaySourceY = 720;
        double walkwaySourceHeight = part1FrontWalkway.getHeight() - walkwaySourceY;
        double walkwayTileWidth = walkwayHeight * part1FrontWalkway.getWidth()
                / walkwaySourceHeight;
        double foregroundOffset = (cameraOffset * 1.00) % walkwayTileWidth;

        double lampSourceY = 475;
        double lampSourceHeight = walkwaySourceY - lampSourceY;
        double lampHeight = walkwayHeight * lampSourceHeight / walkwaySourceHeight;
        double lampY = walkwayTop - lampHeight;
        for (double x = -foregroundOffset; x < WIDTH; x += walkwayTileWidth) {
            gc.drawImage(part1FrontWalkway,
                    0, lampSourceY, part1FrontWalkway.getWidth(), lampSourceHeight,
                    x, lampY, walkwayTileWidth, lampHeight);
            gc.drawImage(part1FrontWalkway,
                    0, walkwaySourceY, part1FrontWalkway.getWidth(), walkwaySourceHeight,
                    x, walkwayTop, walkwayTileWidth, walkwayHeight);
        }
        effects.drawLighting(gc, cameraOffset);
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
        ArcadeHud.label(gc, "PARIS  ·  01:14 AM", 640, 70, 15, Color.web("#ffe7b4"), true);
        ArcadeHud.label(gc, "LAST", 640, 136, 28, Color.web("#fff3d4"), true);
        arcadeTitle(gc, "MÉTRO", 640, 225, 92);
        ArcadeHud.label(gc, "ONE CITY. ONE LAST TRAIN. RUN FOR IT.", 640, 260, 14, Color.web("#fff3d4"), true);
        drawMenuButton(gc, 280, "1  CATCH THE LAST METRO");
        drawMenuButton(gc, 360, "2  ENDLESS COMMUTE");
        drawMenuButton(gc, 440, "H  HOW TO PLAY");
        drawMenuButton(gc, 520, "O  CONTROLS & SETTINGS");
        ArcadeHud.label(gc, "CLICK TO PLAY  ·  OR USE THE NUMBER KEYS", 640, 631, 13, Color.web("#e9e2ce"), true);
        var hero = playerSprites.get("RUNNING");
        if (hero != null) hero.drawGrounded(gc, (int) (menuTime * 4) % hero.getFrameCount(), 170, 580, 190);
        ArcadeHud.panel(gc, 985, 340, 230, 150, false);
        ArcadeHud.label(gc, "THE NIGHT RUN", 1100, 374, 15, Color.web("#ffce76"), true);
        ArcadeHud.label(gc, "500 METRES", 1100, 413, 23, Color.web("#fff4dc"), true);
        ArcadeHud.label(gc, "45 SECONDS · 3 HEARTS", 1100, 449, 12, Color.web("#e2d7bd"), true);
        ArcadeHud.label(gc, "COLLECT · DODGE · CATCH", 1100, 475, 11, Color.web("#e2d7bd"), true);
    }

    private void arcadeTitle(GraphicsContext gc, String text, double x, double y, int size) {
        gc.save(); gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setFont(Font.font(pixelFontFamily, Math.rint(size * .55)));
        gc.setLineWidth(5); gc.setStroke(Color.web("#162332")); gc.strokeText(text, x, y + 4);
        gc.setFill(Color.web("#efcd8b")); gc.fillText(text, x, y);
        gc.restore();
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
        arcadeTitle(gc, "MAKE IT YOUR RUN", 640, 180, 43);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 22));
        ArcadeHud.panel(gc, 360, 250, 560, 80, bindingPrompt.equals("jump"));
        ArcadeHud.panel(gc, 360, 360, 560, 80, bindingPrompt.equals("slide"));
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 22));
        ArcadeHud.label(gc,"JUMP   " + (bindingPrompt.equals("jump") ? "PRESS A KEY..." : jumpKey.toUpperCase()),420,295,22,Color.WHITE,false);
        ArcadeHud.label(gc,"SLIDE   " + (bindingPrompt.equals("slide") ? "PRESS A KEY..." : slideKey.toUpperCase()),420,405,22,Color.WHITE,false);
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Consolas", 17));
        ArcadeHud.label(gc,"CLICK A CONTROL, THEN PRESS A KEY. J / S SELECTS.",315,590,16,Color.LIGHTGRAY,false);
        String note = bindingPrompt.equals("reserved") ? "That key is reserved. Choose a different one."
                : bindingPrompt.equals("unsaved") ? "Could not save settings. Check folder permissions."
                : "F8 / F9 / P / ESC / ENTER reserved. ENTER / ESC returns.";
        ArcadeHud.label(gc,note,335,620,16,Color.LIGHTGRAY,false);
        drawSettingToggle(gc, 460, "F8  SOUND", soundEnabled ? "ON" : "OFF");
        drawSettingToggle(gc, 514, "F9  REDUCED MOTION", effects.isReducedMotion() ? "ON" : "OFF");
    }

    private void drawSettingToggle(GraphicsContext gc,double y,String label,String value) {
        ArcadeHud.panel(gc,360,y,560,42,false);
        ArcadeHud.label(gc,label,385,y+27,18,Color.web("#efcd8b"),false);
        ArcadeHud.label(gc,value,838,y+27,18,Color.web("#f4ebd5"),false);
    }

    private void drawMenuButton(GraphicsContext gc, double y, String label) {
        boolean hover = pointerX >= 360 && pointerX <= 920 && pointerY >= y && pointerY < y + 66;
        ArcadeHud.panel(gc, 360, y, 560, 66, y == 280 || hover);
        ArcadeHud.label(gc, label, 640, y + 41, 21, y == 280 || hover ? Color.web("#263246") : Color.web("#fff0ce"), true);
    }

    private void drawHowToPlay(GraphicsContext gc,String jumpKey,String slideKey) {
        drawMenuBackdrop(gc);ArcadeHud.panel(gc,260,225,760,325,false);
        arcadeTitle(gc,"HOW TO PLAY",640,190,48);
        String[] lines={jumpKey.toUpperCase()+"  HOLD: HIGH JUMP / TAP: SHORT HOP",
                slideKey.toUpperCase()+"  SLIDE / FAST-FALL IN THE AIR","P / ESC  PAUSE OR RESUME",
                "COINS  BUILD A STREAK FOR UP TO 4x POINTS","NAVIGO  FIVE-SECOND COIN MAGNET",
                "HEARTS  RESTORE A LIFE, UP TO THREE","CATCH  500 m IN 45 SECONDS / ENDLESS: SURVIVE"};
        for(int i=0;i<lines.length;i++)ArcadeHud.label(gc,lines[i],300,270+i*39,18,
                i>=3?Color.web("#efcd8b"):Color.web("#f4ebd5"),false);
        drawButton(gc,470,580,340,"BACK TO MENU");
    }

    private void drawGame(GraphicsContext gc, GameWorld world, String bannerText,
                          double bannerTimer, int endlessHighScore) {
        if (world == null) return;
        gc.save();
        if (cameraMotion) gc.translate(effects.cameraX(), effects.cameraY());
        arcade.coins(gc, world, effects.time(), effects.isReducedMotion());
        arcade.hearts(gc, world, effects.time(), effects.isReducedMotion());
        for (Hazard hazard : world.getHazards()) {
            gc.setFill(Color.rgb(15, 17, 25, .24));
            gc.fillOval(hazard.getX() - 4, GROUND_Y - 3, hazard.getWidth() + 8, 7);
            if (arcade.hasProps() && (hazard.getType().equals("BARRIER") || hazard.getType().equals("CRATE"))) {
                gc.setImageSmoothing(false);
                arcade.prop(gc, hazard.getType().equals("BARRIER") ? 1 : 2,
                        hazard.getX() - 3, hazard.getY() + hazard.getHeight(), hazard.getWidth() + 6, hazard.getHeight() + 3);
                if (hazard.getType().equals("BARRIER")) arcade.prop(gc, 3, hazard.getX() + hazard.getWidth() - 5,
                        GROUND_Y, 22, 34);
                continue;
            }
            SpriteStrip sprite = hazardSprites.get(hazard.getType());
            if (sprite != null) {
                int frame = (int) (Math.abs(effects.time() * 8.0 + hazard.getX() / 50.0) % sprite.getFrameCount());
                if (hazard.getType().equals("DOG_POOP")) {
                    sprite.drawFittedMirrored(gc, frame, hazard.getX() - 2, hazard.getY() - 22,
                            hazard.getWidth() + 4, hazard.getHeight() + 25);
                } else if (hazard.getType().equals("PIGEON")) {
                    sprite.drawFittedMirrored(gc, frame, hazard.getX() - 5, hazard.getY() - 4,
                            hazard.getWidth() + 10, hazard.getHeight() + 8);
                } else {
                    sprite.drawFittedMirrored(gc, frame, hazard.getX() - 3, hazard.getY() - 4,
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
                int frame = effects.isReducedMotion() ? 0 : (int) (effects.time() * 3.0) % transitCards.getFrameCount();
                double pulse = 0.16 + (effects.isReducedMotion() ? 0 : 0.07 * Math.sin(effects.time() * 4.0 + ticket.getX() * 0.025));
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

        if (world.isFinaleActive() && !pixelRoute.hasEnding()) drawMetro(gc, world);

        Commuter player = world.getPlayer();
        if (world.getMagnetTime() > 0) {
            gc.setStroke(Color.rgb(129, 212, 246, .42)); gc.setLineWidth(2);
            double radius = effects.isReducedMotion() ? 46 : 46 + Math.sin(effects.time() * 4) * 4;
            gc.strokeOval(player.getX() + 25 - radius, player.getY() + 30 - radius, radius * 2, radius * 2);
        }
        double altitude = Math.max(0, GROUND_Y - player.getY() - player.getHeight());
        gc.setFill(Color.rgb(12, 18, 25, .26 * Math.max(.2, 1 - altitude / 220)));
        gc.fillOval(player.getX() - 9, GROUND_Y - 4, player.getWidth() + 18, 9);
        gc.save();
        double landingScale = effects.landingScale();
        gc.translate(player.getX() + player.getWidth() / 2, player.getY() + player.getHeight());
        gc.scale(1 / landingScale, landingScale);
        gc.translate(-player.getX() - player.getWidth() / 2, -player.getY() - player.getHeight());
        gc.setFill(player.isConfused() ? Color.YELLOWGREEN : player.isSlowed() ? Color.ORANGE : Color.DODGERBLUE);
        double offset = switch (playerAnimation.getCurrentName()) {
            case "RUNNING" -> Math.sin(effects.time() * 27) * 1.3;
            case "SLIDING" -> SLIDING_BOB[playerAnimation.getCurrentFrame()];
            default -> 0;
        };
        if (world.isInvulnerable()) gc.setGlobalAlpha(effects.isReducedMotion() ? .65
                : .55 + .45 * Math.abs(Math.sin(effects.time() * 18)));
        SpriteStrip playerSprite = playerSprites.get(playerAnimation.getCurrentName());
        if (playerSprite == null) {
            gc.fillRect(player.getX(), player.getY() + offset, player.getWidth(), player.getHeight());
        } else {
            double spriteHeight = player.getHeight() * 1.75;
            if (pixelRunner) {
                double height = playerAnimation.getCurrentName().equals("JUMPING") ? 118
                        : playerAnimation.getCurrentName().equals("SLIDING") ? 59 : 143;
                playerSprite.drawGrounded(gc, playerAnimation.getCurrentFrame(), player.getX() + 25,
                        player.getY() + player.getHeight() + offset, height);
            } else playerSprite.drawFitted(gc, playerAnimation.getCurrentFrame(), player.getX() - 26,
                    player.getY() + offset - (spriteHeight - player.getHeight()),
                    player.getWidth() + 52, spriteHeight);
            if (player.isConfused() || player.isSlowed()) {
                gc.setGlobalAlpha(0.2);
                gc.setFill(player.isConfused() ? Color.YELLOWGREEN : Color.ORANGE);
                gc.fillOval(player.getX() - 8, GROUND_Y - 7, player.getWidth() + 16, 12);
            }
        }
        gc.setGlobalAlpha(1.0);
        gc.restore();
        if (!world.isPlayerWon()) effects.drawParticles(gc);
        gc.restore();
        effects.drawAir(gc);

        arcadeHud.draw(gc, world, effects.time(), soundEnabled, effects.isReducedMotion(),
                bannerText, bannerTimer, jumpLabel, slideLabel);
        if (world.isFinaleActive() && !world.isGameOver()) {
            ArcadeHud.label(gc, "BONUS STATION", 640, 190, 15, Color.web("#fff0c8"), true);
            ArcadeHud.label(gc, "LAST TRAIN CAUGHT!", 640, 240, 38, Color.web("#ffd171"), true);
            ArcadeHud.label(gc, "+500", 640, 282, 27, Color.web("#fff0c8"), true);
        }
    }

    private void drawMetro(GraphicsContext gc, GameWorld world) {
        double trainX = world.getMetroTrainX();
        double top = GROUND_Y - 178;
        gc.save();
        gc.setFill(Color.rgb(9, 15, 23, .32)); gc.fillOval(trainX - 10, GROUND_Y - 4, 510, 16);
        gc.setFill(Color.web("#283c43")); gc.fillRoundRect(trainX - 3, top - 9, 486, 20, 10, 10);
        gc.setFill(new LinearGradient(0, top, 0, GROUND_Y, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ddd4b6")), new Stop(.55, Color.web("#a6b7a8")),
                new Stop(1, Color.web("#3d746c"))));
        gc.fillRoundRect(trainX, top, 480, 170, 9, 9);
        gc.setFill(Color.web("#2b7166")); gc.fillRect(trainX, top + 95, 480, 29);
        gc.setFill(Color.web("#e0bc73")); gc.fillRect(trainX, top + 122, 480, 3);
        for (int i = 0; i < 4; i++) {
            double wx = trainX + 160 + i * 77;
            gc.setFill(Color.web("#253e40")); gc.fillRoundRect(wx, top + 24, 63, 65, 5, 5);
            gc.setFill(new LinearGradient(0, top + 27, 0, top + 84, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#cebd88")), new Stop(1, Color.web("#455c57"))));
            gc.fillRoundRect(wx + 4, top + 28, 55, 57, 3, 3);
            gc.setFill(Color.rgb(24, 39, 43, .6));
            gc.fillOval(wx + 20, top + 45, 15, 15); gc.fillRoundRect(wx + 16, top + 59, 24, 27, 8, 8);
            gc.setStroke(Color.rgb(255, 241, 194, .35)); gc.setLineWidth(2);
            gc.strokeLine(wx + 7, top + 33, wx + 30, top + 33);
        }
        gc.setFill(Color.web("#263c3f")); gc.fillRect(trainX + 36, top + 19, 99, 142);
        gc.setFill(Color.web("#e4c58b")); gc.fillRect(trainX + 41, top + 23, 89, 135);
        double doorWidth = 45 * world.getMetroDoorProgress();
        for (double dx : new double[] {trainX + 40, trainX + 130 - doorWidth}) {
            gc.setFill(Color.web("#859e8e")); gc.fillRect(dx, top + 22, doorWidth, 138);
            if (doorWidth > 9) {
                gc.setFill(Color.web("#2d4749")); gc.fillRect(dx + 4, top + 32, doorWidth - 8, 59);
                gc.setStroke(Color.web("#c3cfb8")); gc.strokeRect(dx, top + 22, doorWidth, 138);
            }
        }
        gc.setFill(Color.web("#192e34")); gc.fillRect(trainX + 41, top + 4, 88, 13);
        gc.setFill(Color.web("#f7cf83")); gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
        gc.fillText("DERNIERE RAME", trainX + 46, top + 14);
        for (int i = 0; i < 4; i++) {
            gc.setFill(Color.web("#1e2c35")); gc.fillOval(trainX + 20 + i * 135, GROUND_Y - 18, 28, 22);
            gc.setFill(Color.web("#748381")); gc.fillOval(trainX + 27 + i * 135, GROUND_Y - 13, 14, 12);
        }
        gc.restore();
    }

    private void drawPause(GraphicsContext gc, GameWorld world) {
        drawOverlayPanel(gc);
        arcadeTitle(gc, "TAKE A BREATHER", 640, 270, 42);
        ArcadeHud.label(gc, String.format("%d COINS   ·   %,d POINTS", world.getCoinsCollected(), world.getScore()),
                640, 326, 22, Color.web("#fff0ce"), true);
        ArcadeHud.label(gc, "P / ESC RESUMES  ·  M RETURNS TO THE MENU", 640, 370, 14, Color.web("#dcd7c8"), true);
        drawButton(gc, 300, 410, 320, "RESUME");
        drawButton(gc, 660, 410, 320, "MAIN MENU");
    }

    private void drawResults(GraphicsContext gc, GameWorld world, String reason, int endlessHighScore) {
        drawOverlayPanel(gc);
        arcadeTitle(gc, world.isPlayerWon() ? "LEVEL COMPLETE!" : "MISSED THE LAST TRAIN", 640, 250, world.isPlayerWon() ? 48 : 37);
        ArcadeHud.label(gc, world.isPlayerWon() ? "WELCOME ABOARD. PARIS CAN WAIT." : "CATCH YOUR BREATH. GO AGAIN.",
                640, 292, 14, Color.web("#eadfca"), true);
        ArcadeHud.label(gc, String.format("%,d", world.getScore()), 640, 355, 47, Color.web("#ffd477"), true);
        ArcadeHud.label(gc, "POINTS", 640, 378, 12, Color.web("#eadfca"), true);
        ArcadeHud.label(gc, String.format("%d COINS  ·  %.0f m  ·  BEST STREAK %d", world.getCoinsCollected(), world.getDistanceMeters(), world.getBestCombo()),
                640, 414, 18, Color.web("#fff0ce"), true);
        ArcadeHud.label(gc, world.isPlayerWon() ? "+500 LAST TRAIN BONUS" : world.getMode() == GameWorld.Mode.ENDLESS
                ? "PERSONAL BEST  " + endlessHighScore : "R TO RETRY  ·  M FOR MENU", 640, 452, 14, Color.web("#ffcf77"), true);
        drawButton(gc, 300, 485, 320, "TRY AGAIN");
        drawButton(gc, 660, 485, 320, "MAIN MENU");
    }

    private void drawOverlayPanel(GraphicsContext gc) {
        gc.setFill(Color.rgb(10, 15, 26, 0.50));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        ArcadeHud.panel(gc, 265, 173, 750, 414, false);
    }

    private void drawButton(GraphicsContext gc, double x, double y, double width, String label) {
        boolean highlight = x == 300 || (pointerX >= x && pointerX <= x + width && pointerY >= y && pointerY < y + 64);
        ArcadeHud.panel(gc, x, y, width, 64, highlight);
        ArcadeHud.label(gc, label, x + width / 2, y + 40, 20, highlight ? Color.web("#263246") : Color.web("#fff0ce"), true);
    }

    private void drawTransition(GraphicsContext gc, double alpha) {
        if (alpha <= 0.0) return;
        gc.setFill(Color.rgb(0, 0, 0, alpha));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
    }
}
