package com.lastmetro;

import com.lastmetro.model.Commuter;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    public static final double GROUND_Y = 580.0;

    private Commuter player;

    @Override
    public void start(Stage stage) {
        player = new Commuter(150, GROUND_Y);

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Pane root = new Pane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        // Arcade Keyboard Controls
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP || e.getCode() == KeyCode.Z) {
                player.onJumpPressed();
            } else if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.S) {
                player.onSlideChanged(true);
            }
        });

        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP || e.getCode() == KeyCode.Z) {
                player.onJumpReleased();
            } else if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.S) {
                player.onSlideChanged(false);
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
                // Clamp deltaTime so alt-tabbing doesn't break physics
                double deltaTime = Math.min((now - lastTime) / 1_000_000_000.0, 0.05);
                lastTime = now;

                update(deltaTime);
                render(gc);
            }
        }.start();
    }

    private void update(double deltaTime) {
        player.update(deltaTime);
    }

    private void render(GraphicsContext gc) {
        // Dark Parisian night background
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Ground (Paris sidewalk)
        gc.setFill(Color.web("#4a4e69"));
        gc.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Player (changes color slightly when sliding so you can feel the state switch)
        gc.setFill(player.getCurrentState().getStateName().equals("SLIDING")
                ? Color.DEEPSKYBLUE
                : Color.DODGERBLUE);
        gc.fillRect(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        // Debug HUD for state verification
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 18));
        gc.fillText("STATE: " + player.getCurrentState().getStateName() + " | [SPACE/Z] Jump  [DOWN/S] Slide", 20, 35);
    }

    public static void main(String[] args) {
        launch(args);
    }
}