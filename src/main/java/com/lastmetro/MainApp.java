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
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.nio.file.Path;

public class MainApp extends Application implements GameObserver {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    public static final double GROUND_Y = 580.0;

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

    private final GameRenderer renderer = new GameRenderer();
    private final FixedStepAccumulator simulationClock = new FixedStepAccumulator();
    private final ScreenTransition screenTransition = new ScreenTransition(0.16);
    private final GameViewport viewport = new GameViewport(WIDTH, HEIGHT);

    @Override
    public void start(Stage stage) {
        renderer.loadAssets();
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        Pane root = new Pane(canvas);
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        scene.setOnKeyPressed(event -> handleKeyPressed(event.getCode()));
        scene.setOnKeyReleased(event -> {
            if (screen != GameScreen.PLAYING || screenTransition.isRunning() || world.isFinaleActive()) return;
            if (isJumpKey(event.getCode())) world.getPlayer().onJumpReleased();
            else if (isSlideKey(event.getCode())) world.getPlayer().onSlideChanged(false);
        });
        scene.setOnMouseClicked(event -> handleMouseClick(event.getX(), event.getY()));

        stage.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused && world != null) {
                world.getPlayer().releaseInputs();
                simulationClock.reset();
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
                renderer.updateUi(deltaTime);

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
            renderer.update(world, step);
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
        renderer.render(gc, screen, world, bannerText, bannerTimer, gameOverReason,
                endlessHighScore, settings.jumpKey().getName(), settings.slideKey().getName(),
                bindingPrompt, screenTransition.getAlpha());
        gc.restore();
    }

    private void handleMouseClick(double x, double y) {
        if (screenTransition.isRunning() || !viewport.contains(x, y)) return;
        double logicalX = viewport.toLogicalX(x);
        double logicalY = viewport.toLogicalY(y);
        if (screen == GameScreen.HOW_TO_PLAY) {
            if (logicalX >= 470 && logicalX < 810 && logicalY >= 580 && logicalY < 644) {
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
        if (screen == GameScreen.RESULTS) {
            if (logicalY >= 485 && logicalY < 549 && logicalX >= 300 && logicalX < 620) {
                transitionTo(() -> startGame(world.getMode()));
            } else if (logicalY >= 485 && logicalY < 549 && logicalX >= 660 && logicalX < 980) {
                transitionTo(() -> screen = GameScreen.MENU);
            }
            return;
        }
        if (screen != GameScreen.MENU && screen != GameScreen.SETTINGS) return;
        if (screen == GameScreen.SETTINGS) {
            if (logicalX >= 360 && logicalX < 920 && logicalY >= 250 && logicalY < 330) bindingPrompt = "jump";
            else if (logicalX >= 360 && logicalX < 920 && logicalY >= 360 && logicalY < 440) bindingPrompt = "slide";
            return;
        }
        if (logicalX >= 360 && logicalX < 920 && logicalY >= 280 && logicalY < 346) {
            transitionTo(() -> startGame(GameWorld.Mode.CATCH_THE_METRO));
        } else if (logicalX >= 360 && logicalX < 920 && logicalY >= 360 && logicalY < 426) {
            transitionTo(() -> startGame(GameWorld.Mode.ENDLESS));
        } else if (logicalX >= 360 && logicalX < 920 && logicalY >= 440 && logicalY < 506) {
            transitionTo(() -> screen = GameScreen.HOW_TO_PLAY);
        } else if (logicalX >= 360 && logicalX < 920 && logicalY >= 520 && logicalY < 586) {
            openSettings();
        }
    }

    private void handleKeyPressed(KeyCode code) {
        if (screenTransition.isRunning()) return;
        if (screen == GameScreen.MENU) {
            if (code == KeyCode.DIGIT1 || code == KeyCode.NUMPAD1 || code == KeyCode.ENTER) {
                transitionTo(() -> startGame(GameWorld.Mode.CATCH_THE_METRO));
            } else if (code == KeyCode.DIGIT2 || code == KeyCode.NUMPAD2) {
                transitionTo(() -> startGame(GameWorld.Mode.ENDLESS));
            } else if (code == KeyCode.H) {
                transitionTo(() -> screen = GameScreen.HOW_TO_PLAY);
            } else if (code == KeyCode.O) {
                openSettings();
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
            world.getPlayer().onJumpPressed();
        } else if (isSlideKey(code)) {
            world.getPlayer().onSlideChanged(true);
        }
    }

    private void openSettings() {
        transitionTo(() -> {
            bindingPrompt = "";
            screen = GameScreen.SETTINGS;
        });
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

    private void startGame(GameWorld.Mode mode) {
        world = new GameWorld(WIDTH, GROUND_Y, mode);
        world.addObserver(this);
        simulationClock.reset();
        renderer.resetAnimations();
        screen = GameScreen.PLAYING;
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

    @Override
    public void onSarcasticEvent(String message) {
        bannerText = message;
        bannerTimer = 3.0;
    }

    @Override
    public void onFinaleTriggered(String reason) {
        gameOverReason = reason;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
