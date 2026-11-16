package com.lastmetro;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Pane root = new Pane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        stage.setTitle("Last Métro - Ligne 13 Edition");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // 60 FPS Game Loop
        new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }
                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                update(deltaTime);
                render(gc);
            }
        }.start();
    }

    private void update(double deltaTime) {
        // Physics & game logic will go here
    }

    private void render(GraphicsContext gc) {
        // Dark Parisian night background
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Ground (Paris sidewalk)
        gc.setFill(Color.web("#4a4e69"));
        gc.fillRect(0, 580, WIDTH, 140);

        // Temporary placeholder player (Blue Rectangle)
        gc.setFill(Color.DODGERBLUE);
        gc.fillRect(150, 500, 50, 80);
    }

    public static void main(String[] args) {
        launch(args);
    }
}