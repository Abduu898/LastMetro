package com.lastmetro;

import com.lastmetro.model.Commuter;
import com.lastmetro.model.GameWorld;
import com.lastmetro.model.Hazard;
import com.lastmetro.patterns.observer.GameObserver;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainApp extends Application implements GameObserver {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    public static final double GROUND_Y = 580.0;

    private GameWorld world;
    private String bannerText = "01:14 AM — Ligne 13 ferme dans 45s ! [SPACE] Sauter  [DOWN] Glisser";
    private double bannerTimer = 5.0;
    private String gameOverReason = "";

    @Override
    public void start(Stage stage) {
        world = new GameWorld(WIDTH, GROUND_Y);
        world.addObserver(this);

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Pane root = new Pane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        scene.setOnKeyPressed(e -> {
            if (world.isGameOver() && e.getCode() == KeyCode.R) {
                world.reset();
                bannerText = "Nouvelle tentative. La RATP croit en toi (non).";
                bannerTimer = 3.5;
                return;
            }
            if (world.isFinaleActive()) return;

            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP || e.getCode() == KeyCode.Z) {
                world.getPlayer().onJumpPressed();
            } else if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.S) {
                world.getPlayer().onSlideChanged(true);
            }
        });

        scene.setOnKeyReleased(e -> {
            if (world.isFinaleActive()) return;

            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP || e.getCode() == KeyCode.Z) {
                world.getPlayer().onJumpReleased();
            } else if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.S) {
                world.getPlayer().onSlideChanged(false);
            }
        });

        stage.setTitle("Last Métro - Ligne 13 Edition");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }
                double deltaTime = Math.min((now - lastTime) / 1_000_000_000.0, 0.05);
                lastTime = now;

                if (bannerTimer > 0) bannerTimer = Math.max(0, bannerTimer - deltaTime);
                world.update(deltaTime);
                render(gc);
            }
        }.start();
    }

    @Override
    public void onSarcasticEvent(String message) {
        this.bannerText = message;
        this.bannerTimer = 3.5;
    }

    @Override
    public void onFinaleTriggered(String reason) {
        this.gameOverReason = reason;
    }

    private void render(GraphicsContext gc) {
        // Parisian night sky
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Sidewalk / Platform
        gc.setFill(Color.web("#4a4e69"));
        gc.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Render Hazards
        for (Hazard h : world.getHazards()) {
            switch (h.getType()) {
                case "DOG_POOP" -> gc.setFill(Color.SADDLEBROWN);
                case "E_SCOOTER" -> gc.setFill(Color.LIMEGREEN);
                case "PIGEON" -> gc.setFill(Color.LIGHTSLATEGRAY);
                default -> gc.setFill(Color.CRIMSON);
            }
            gc.fillRect(h.getX(), h.getY(), h.getWidth(), h.getHeight());
        }

        // Render Metro Train during 00:00 Finale
        if (world.isFinaleActive()) {
            double trainX = world.getMetroTrainX();
            gc.setFill(Color.web("#e0e1dd"));
            gc.fillRect(trainX, GROUND_Y - 160, 480, 160);
            gc.setFill(Color.web("#00b4d8")); // RATP turquoise stripe
            gc.fillRect(trainX, GROUND_Y - 70, 480, 22);

            // Doors slamming shut
            double doorWidth = 45 * world.getMetroDoorProgress();
            gc.setFill(Color.DARKSLATEGRAY);
            gc.fillRect(trainX + 40, GROUND_Y - 135, doorWidth, 130);
            gc.fillRect(trainX + 130 - doorWidth, GROUND_Y - 135, doorWidth, 130);
        }

        // Render Commuter
        Commuter p = world.getPlayer();
        if (p.isConfused()) {
            gc.setFill(Color.YELLOWGREEN); // Stench debuff tint
        } else if (p.isSlowed()) {
            gc.setFill(Color.ORANGE);
        } else {
            gc.setFill(Color.DODGERBLUE);
        }
        gc.fillRect(p.getX(), p.getY(), p.getWidth(), p.getHeight());

        // HUD: RATP Countdown & Distance
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
        gc.fillText(String.format("MÉTRO DANS : 00:%05.2f", world.getTimeRemaining()), 25, 42);
        gc.fillText(String.format("DISTANCE : %.0f m", world.getDistanceMeters()), WIDTH - 260, 42);

        // Active Debuff Indicator
        if (p.isConfused()) {
            gc.setFill(Color.YELLOWGREEN);
            gc.fillText("DEBUFF: ODEUR SUSPECTE (TOUCHES INVERSÉES !)", 25, 76);
        } else if (p.isSlowed()) {
            gc.setFill(Color.ORANGE);
            gc.fillText("DEBUFF: RALENTI", 25, 76);
        }

        // Sarcastic Pop-up Banner
        if (bannerTimer > 0 && !bannerText.isEmpty()) {
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(240, 95, 800, 44);
            gc.setFill(Color.GOLD);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 17));
            gc.fillText(bannerText, 260, 123);
        }

        // Game Over Receipt Overlay
        if (world.isGameOver()) {
            gc.setFill(Color.rgb(10, 10, 20, 0.85));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
            gc.setFill(Color.CRIMSON);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 42));
            gc.fillText("MÉTRO RATÉ — FIN DE SERVICE", 310, 270);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", 22));
            gc.fillText(gameOverReason, 260, 330);
            gc.fillText(String.format("Distance parcourue : %.0f mètres pour rien.", world.getDistanceMeters()), 360, 380);

            gc.setFill(Color.GOLD);
            gc.fillText("Appuie sur [R] pour souffrir à nouveau", 410, 460);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}