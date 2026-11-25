package com.lastmetro;

import com.lastmetro.animation.FixedStepAccumulator;
import com.lastmetro.animation.ScreenTransition;
import com.lastmetro.model.GameWorld;
import com.lastmetro.persistence.HighScoreStore;
import com.lastmetro.persistence.GameSettings;
import com.lastmetro.persistence.GameSettingsStore;
import com.lastmetro.patterns.observer.GameObserver;
import com.lastmetro.view.GameRenderer;
import com.lastmetro.view.GameScreen;
import com.lastmetro.view.GameViewport;
import com.lastmetro.view.ArcadeHud;
import com.lastmetro.view.VictoryTicket;
import com.lastmetro.view.TicketKiosk;
import com.lastmetro.audio.GameAudio;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.nio.file.Path;

public class MainApp extends Application implements GameObserver {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    public static final double GROUND_Y = 576.0;

    private GameWorld world;
    private GameScreen screen = GameScreen.MENU;
    private String bannerText = "";
    private double bannerTimer = 0.0;
    private String gameOverReason = "";
    private final HighScoreStore highScoreStore = new HighScoreStore(
            Path.of(System.getProperty("user.home"), ".lastmetro-highscore"));
    private final GameSettingsStore settingsStore = new GameSettingsStore(
            Path.of(System.getProperty("user.home"), ".lastmetro", "settings.properties"));
    private GameSettings settings = settingsStore.load();
    private String bindingPrompt = "";
    private int endlessHighScore = highScoreStore.load();
    private GameAudio audio;
    private Rectangle2D mousePressedTicketButton;
    private int menuSelection, pressedMenuOption = -1;
    private final java.util.Set<KeyCode> heldKeys = new java.util.HashSet<>();

    private final GameRenderer renderer = new GameRenderer();
    private final FixedStepAccumulator simulationClock = new FixedStepAccumulator();
    private final ScreenTransition screenTransition = new ScreenTransition(0.16);
    private final GameViewport viewport = new GameViewport(WIDTH, HEIGHT);

    @Override
    public void start(Stage stage) {
        renderer.loadAssets();
        renderer.setPresentationSettings(settings.soundEnabled(), settings.reducedMotion());
        audio = new GameAudio(settings.soundEnabled());
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        Pane root = new Pane(canvas);
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        scene.setOnKeyPressed(event -> {
            if (heldKeys.add(event.getCode())) handleKeyPressed(event.getCode());
        });
        scene.setOnKeyReleased(event -> {
            boolean wasHeld = heldKeys.remove(event.getCode());
            if (wasHeld && isVictoryTicket() && !screenTransition.isRunning()) {
                Rectangle2D action = ticketShortcut(event.getCode());
                if (action != null) {
                    activateTicketAction(action);
                    return;
                }
            }
            if (screen != GameScreen.PLAYING || screenTransition.isRunning() || world.isFinaleActive() || world.isTransferring()) return;
            if (isJumpKey(event.getCode())) world.getPlayer().onJumpReleased();
            else if (isSlideKey(event.getCode())) world.getPlayer().onSlideChanged(false);
        });
        scene.setOnMousePressed(event -> {
            if (isVictoryTicket() && !screenTransition.isRunning() && event.getButton() == MouseButton.PRIMARY)
                mousePressedTicketButton = ticketButtonAt(viewport.toLogicalX(event.getX()), viewport.toLogicalY(event.getY()));
            if (screen == GameScreen.MENU && !screenTransition.isRunning() && event.getButton() == MouseButton.PRIMARY)
                pressedMenuOption = TicketKiosk.optionAt(viewport.toLogicalX(event.getX()), viewport.toLogicalY(event.getY()));
        });
        scene.setOnMouseReleased(event -> {
            mousePressedTicketButton = null;
            if (!screenTransition.isRunning()) pressedMenuOption = -1;
        });
        scene.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY)
                handleMouseClick(event.getX(), event.getY());
        });
        scene.setOnMouseMoved(event -> {
            double x = viewport.toLogicalX(event.getX()), y = viewport.toLogicalY(event.getY());
            renderer.setPointer(x, y);
            int option = TicketKiosk.optionAt(x, y);
            if (screen == GameScreen.MENU && option >= 0 && !screenTransition.isRunning()) selectMenuOption(option, true);
            boolean pointer = screen == GameScreen.PLAYING ? ArcadeHud.hitsPause(x, y)
                    : screen == GameScreen.MENU ? option >= 0
                    : screen == GameScreen.HOW_TO_PLAY ? TicketKiosk.BACK.contains(x, y)
                    : screen == GameScreen.SETTINGS ? TicketKiosk.BACK.contains(x, y)
                    || TicketKiosk.JUMP.contains(x, y) || TicketKiosk.SLIDE.contains(x, y)
                    || TicketKiosk.SOUND.contains(x, y) || TicketKiosk.MOTION.contains(x, y)
                    : isVictoryTicket() ? ticketButtonAt(x, y) != null
                    : screen == GameScreen.PAUSED || screen == GameScreen.RESULTS;
            scene.setCursor(pointer ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);
        });

        stage.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) {
                heldKeys.clear();
                mousePressedTicketButton = null;
                if (!screenTransition.isRunning()) pressedMenuOption = -1;
            }
            if (!isFocused && world != null) {
                world.getPlayer().releaseInputs();
                simulationClock.reset();
                if (screen == GameScreen.PLAYING && !world.isGameOver()) {
                    screenTransition.cancel();
                    screen = GameScreen.PAUSED;
                    audio.setActive(false);
                }
            }
        });

        stage.setTitle("Last Métro — Dernière Rame");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
        root.requestFocus();

        new AnimationTimer() {
            private long lastTime;

            @Override
            public void handle(long now) {
                double deltaTime = lastTime == 0 ? 0 : (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                screenTransition.update(Math.min(deltaTime, 0.25));
                renderer.updateUi(Math.min(deltaTime, .1), screen == GameScreen.RESULTS);
                audio.setActive(screen == GameScreen.PLAYING || screen == GameScreen.RESULTS);
                if (isVictoryTicket()) audio.playTicketCue(renderer.updateVictory(Math.min(deltaTime, .1)));

                if (screen == GameScreen.PLAYING && world != null && !screenTransition.isRunning()) {
                    updateSimulation(deltaTime);
                } else {
                    simulationClock.reset();
                }

                drawFrame(canvas, canvas.getGraphicsContext2D());
            }
        }.start();
    }

    private void updateSimulation(double frameDelta) {
        int steps = simulationClock.advance(frameDelta);
        for (int i = 0; i < steps && !world.isGameOver(); i++) {
            double step = FixedStepAccumulator.STEP_SECONDS;
            if (bannerTimer > 0) bannerTimer = Math.max(0, bannerTimer - step);
            world.update(step);
            for (var event : renderer.update(world, step)) audio.event(event);
        }

        if (world.isGameOver() && !screenTransition.isRunning()) {
            if (world.getMode() == GameWorld.Mode.ENDLESS) {
                endlessHighScore = highScoreStore.saveIfHigher(world.getScore());
            }
            transitionTo(() -> screen = GameScreen.RESULTS);
        }
    }

    private void drawFrame(Canvas canvas, GraphicsContext gc) {
        viewport.resize(canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (viewport.getScale() <= 0.0) return;

        gc.save();
        gc.translate(viewport.getOffsetX(), viewport.getOffsetY());
        gc.scale(viewport.getScale(), viewport.getScale());
        Rectangle2D pressed = isVictoryTicket() ? mousePressedTicketButton : null;
        if (isVictoryTicket() && pressed == null && !screenTransition.isRunning()) {
            for (KeyCode key : heldKeys) {
                pressed = ticketShortcut(key);
                if (pressed != null) break;
            }
        }
        renderer.setPressedResultButton(pressed);
        renderer.setKioskSelection(menuSelection, pressedMenuOption);
        renderer.render(gc, screen, world, bannerText, bannerTimer, gameOverReason,
                endlessHighScore, settings.jumpKey().getName(), settings.slideKey().getName(),
                bindingPrompt, screenTransition.getAlpha());
        gc.restore();
    }

    private void handleMouseClick(double x, double y) {
        if (screenTransition.isRunning() || !viewport.contains(x, y)) return;
        double logicalX = viewport.toLogicalX(x);
        double logicalY = viewport.toLogicalY(y);
        if (screen == GameScreen.PLAYING && ArcadeHud.hitsPause(logicalX, logicalY)) {
            handleKeyPressed(KeyCode.P); return;
        }
        if (screen == GameScreen.HOW_TO_PLAY) {
            if (TicketKiosk.BACK.contains(logicalX, logicalY)) {
                transitionTo(() -> screen = GameScreen.MENU);
            }
            return;
        }
        if (screen == GameScreen.PAUSED) {
            if (logicalY >= 410 && logicalY < 474 && logicalX >= 300 && logicalX < 620) {
                transitionTo(() -> screen = GameScreen.PLAYING);
            } else if (logicalY >= 410 && logicalY < 474 && logicalX >= 660 && logicalX < 980) {
                transitionTo(() -> screen = GameScreen.MENU);
            }
            return;
        }
        if (isVictoryTicket()) {
            Rectangle2D action = ticketButtonAt(logicalX, logicalY);
            if (action != null) activateTicketAction(action);
            return;
        }
        if (screen == GameScreen.RESULTS) {
            if (logicalY >= 485 && logicalY < 549 && logicalX >= 300 && logicalX < 620) {
                transitionTo(() -> startGame(world.getMode()));
            } else if (logicalY >= 485 && logicalY < 549 && logicalX >= 660 && logicalX < 980) {
                transitionTo(() -> screen = GameScreen.MENU);
            }
            return;
        }
        if (screen == GameScreen.SETTINGS) {
            if (TicketKiosk.JUMP.contains(logicalX, logicalY)) bindingPrompt = "jump";
            else if (TicketKiosk.SLIDE.contains(logicalX, logicalY)) bindingPrompt = "slide";
            else if (TicketKiosk.SOUND.contains(logicalX, logicalY)) toggleSound();
            else if (TicketKiosk.MOTION.contains(logicalX, logicalY)) toggleMotion();
            else if (TicketKiosk.BACK.contains(logicalX, logicalY)) transitionTo(() -> screen = GameScreen.MENU);
        } else if (screen == GameScreen.MENU) {
            int option = TicketKiosk.optionAt(logicalX, logicalY);
            if (option >= 0) activateMenuOption(option);
        }
    }

    private void selectMenuOption(int option, boolean tick) {
        if (menuSelection != option && tick) audio.playKioskCue(false);
        menuSelection = option;
    }

    private void activateMenuOption(int option) {
        selectMenuOption(option, false);
        pressedMenuOption = option;
        audio.playKioskCue(true);
        transitionTo(() -> {
            pressedMenuOption = -1;
            switch (option) {
                case 0 -> startGame(GameWorld.Mode.CATCH_THE_METRO);
                case 1 -> startGame(GameWorld.Mode.ENDLESS);
                case 2 -> screen = GameScreen.HOW_TO_PLAY;
                case 3 -> {
                    bindingPrompt = "";
                    screen = GameScreen.SETTINGS;
                }
                default -> throw new IllegalArgumentException("Unknown kiosk option");
            }
        });
    }

    private void handleKeyPressed(KeyCode code) {
        if (code == KeyCode.F8) { toggleSound(); return; }
        if (code == KeyCode.F9) { toggleMotion(); return; }
        if (screenTransition.isRunning()) return;
        if (screen == GameScreen.MENU) {
            switch (code) {
                case UP, W -> selectMenuOption(Math.floorMod(menuSelection - 1, TicketKiosk.OPTIONS), true);
                case DOWN, S -> selectMenuOption((menuSelection + 1) % TicketKiosk.OPTIONS, true);
                case ENTER, SPACE -> activateMenuOption(menuSelection);
                case DIGIT1, NUMPAD1 -> activateMenuOption(0);
                case DIGIT2, NUMPAD2 -> activateMenuOption(1);
                case H -> activateMenuOption(2);
                case O -> activateMenuOption(3);
                default -> {}
            }
            return;
        }
        if (screen == GameScreen.HOW_TO_PLAY) {
            if (code == KeyCode.ESCAPE || code == KeyCode.ENTER) transitionTo(() -> screen = GameScreen.MENU);
            return;
        }
        if (screen == GameScreen.SETTINGS) {
            if (bindingPrompt.equals("jump") || bindingPrompt.equals("slide")) {
                if (code == KeyCode.ESCAPE) bindingPrompt = "";
                else bindKey(code);
            } else if (code == KeyCode.ESCAPE || code == KeyCode.ENTER) {
                transitionTo(() -> screen = GameScreen.MENU);
            } else if (code == KeyCode.J) bindingPrompt = "jump";
            else if (code == KeyCode.S) bindingPrompt = "slide";
            return;
        }
        if (screen == GameScreen.PAUSED) {
            if (code == KeyCode.P || code == KeyCode.ESCAPE) {
                transitionTo(() -> screen = GameScreen.PLAYING);
            } else if (code == KeyCode.M) {
                transitionTo(() -> screen = GameScreen.MENU);
            }
            return;
        }
        if (isVictoryTicket()) return; // Commit ticket actions on key release so the bevel can depress.
        if (screen == GameScreen.RESULTS) {
            if (code == KeyCode.R) {
                transitionTo(() -> startGame(world.getMode()));
            } else if (code == KeyCode.M || code == KeyCode.ESCAPE) {
                transitionTo(() -> screen = GameScreen.MENU);
            }
            return;
        }

        if (code == KeyCode.P || code == KeyCode.ESCAPE) {
            transitionTo(() -> {
                world.getPlayer().releaseInputs();
                screen = GameScreen.PAUSED;
            });
        } else if (isJumpKey(code)) {
            if (world.isFinaleActive() || world.isTransferring()) return;
            world.getPlayer().onJumpPressed();
        } else if (isSlideKey(code)) {
            if (world.isFinaleActive() || world.isTransferring()) return;
            world.getPlayer().onSlideChanged(true);
        }
    }

    private boolean isVictoryTicket() {
        return screen == GameScreen.RESULTS && world != null && world.isPlayerWon();
    }

    private static Rectangle2D ticketButtonAt(double x, double y) {
        if (VictoryTicket.RETRY.contains(x, y)) return VictoryTicket.RETRY;
        return VictoryTicket.MENU.contains(x, y) ? VictoryTicket.MENU : null;
    }

    private static Rectangle2D ticketShortcut(KeyCode key) {
        return switch (key) {
            case R, ENTER, SPACE -> VictoryTicket.RETRY;
            case M, ESCAPE -> VictoryTicket.MENU;
            default -> null;
        };
    }

    private void activateTicketAction(Rectangle2D action) {
        if (action == VictoryTicket.RETRY) transitionTo(() -> startGame(world.getMode()));
        else transitionTo(() -> screen = GameScreen.MENU);
    }

    private boolean isJumpKey(KeyCode code) {
        return code == settings.jumpKey();
    }

    private boolean isSlideKey(KeyCode code) {
        return code == settings.slideKey();
    }

    private void bindKey(KeyCode code) {
        try {
            settings = bindingPrompt.equals("jump") ? settings.withJumpKey(code) : settings.withSlideKey(code);
            boolean saved = settingsStore.save(settings);
            bindingPrompt = "";
            if (!saved) {
                bindingPrompt = "unsaved";
            }
        } catch (IllegalArgumentException ignored) {
            bindingPrompt = "reserved";
        }
    }

    private boolean catchAttempted;
    private boolean endlessAttempted;

    private void startGame(GameWorld.Mode mode) {
        world = new GameWorld(WIDTH, GROUND_Y, mode);
        if (mode == GameWorld.Mode.CATCH_THE_METRO) {
            if (catchAttempted) world.useQuickTransfers();
            catchAttempted = true;
        } else {
            if (endlessAttempted) world.useQuickTransfers();
            endlessAttempted = true;
        }
        world.addObserver(this);
        simulationClock.reset();
        renderer.resetAnimations();
        screen = GameScreen.PLAYING;
        audio.setActive(true);
        audio.startRun(mode == GameWorld.Mode.CATCH_THE_METRO);
        bannerText = mode == GameWorld.Mode.ENDLESS
                ? "Endless Commute — survive and set a distance record!"
                : "Catch the Last Metro — reach 500 m before time runs out!";
        bannerTimer = 4.0;
        gameOverReason = "";
    }

    private void transitionTo(Runnable action) {
        simulationClock.reset();
        screenTransition.start(action);
    }

    private void toggleSound() {
        settings = settings.withSoundEnabled(!settings.soundEnabled());
        audio.setEnabled(settings.soundEnabled());
        savePresentationSettings();
    }

    private void toggleMotion() {
        settings = settings.withReducedMotion(!settings.reducedMotion());
        savePresentationSettings();
    }

    private void savePresentationSettings() {
        renderer.setPresentationSettings(settings.soundEnabled(), settings.reducedMotion());
        if (!settingsStore.save(settings)) bindingPrompt = "unsaved";
    }

    @Override public void stop() { if (audio != null) audio.close(); }

    @Override
    public void onSarcasticEvent(String message) {
        bannerText = message;
        bannerTimer = message.contains("HEARTS:") || message.startsWith("CLEAN SECTION") ? 2.0 : 1.2;
        if (message.contains("HEARTS:")) bannerText = "HIT  ·  " + world.getHearts() + " / " + GameWorld.MAX_HEARTS + " HEARTS";
    }

    @Override
    public void onFinaleTriggered(String reason) {
        gameOverReason = reason;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
