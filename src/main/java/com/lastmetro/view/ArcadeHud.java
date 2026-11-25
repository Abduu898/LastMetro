package com.lastmetro.view;

import com.lastmetro.model.GameWorld;
import com.lastmetro.model.RouteTimeline;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.*;
import javafx.scene.text.*;

/** Compact pixel-art HUD. Its pause target is shared with input handling. */
public final class ArcadeHud {
    public static final double PAUSE_X = 1208, PAUSE_Y = 16, PAUSE_SIZE = 52;
    private Image portrait;
    private static String pixelFont = "Monospaced";
    private final ArcadeArt art;
    private static final Color GOLD = Color.web("#ffcf64"), IVORY = Color.web("#fff2d5");
    public ArcadeHud(ArcadeArt art) { this.art = art; }
    public void load() {
        var font = ArcadeHud.class.getResource("/fonts/PressStart2P-Regular.ttf");
        if (font != null) { var loaded = Font.loadFont(font.toExternalForm(), 12); if (loaded != null) pixelFont = loaded.getFamily(); }
        var url = ArcadeHud.class.getResource("/assets/pixel-route/commuter.png");
        if (url != null) portrait = new Image(url.toExternalForm());
    }
    public static boolean hitsPause(double x, double y) {
        return x >= PAUSE_X && x <= PAUSE_X + PAUSE_SIZE && y >= PAUSE_Y && y <= PAUSE_Y + PAUSE_SIZE;
    }
    public void draw(GraphicsContext gc, GameWorld world, double time, boolean sound, boolean lowMotion,
                     String banner, double bannerTimer, String jump, String slide) {
        gc.save(); gc.setImageSmoothing(false);
        gc.setFill(new LinearGradient(0, 0, 0, 150, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(15, 20, 31, .45)), new Stop(1, Color.TRANSPARENT)));
        gc.fillRect(0, 0, 1280, 150);
        panel(gc, 16, 14, 66, 70, false);
        if (portrait != null) {
            gc.drawImage(portrait, 238, 45, 104, 118, 23, 19, 52, 59);
        }
        for (int i = 0; i < 3; i++) heart(gc, 99 + i * 35, 20, i < world.getHearts());
        art.prop(gc, 0, 101, 83, 24, 25);
        label(gc, String.format("%03d", world.getCoinsCollected()), 135, 79, 22, IVORY, false);
        if (world.getMultiplier() > 1) label(gc, "×" + world.getMultiplier(), 196, 78, 21, GOLD, false);

        panel(gc, 548, 15, 184, 54, false);
        gc.setStroke(GOLD); gc.setLineWidth(2.3); gc.strokeOval(563, 30, 24, 25);
        gc.strokeLine(575, 32, 575, 43); gc.strokeLine(575, 43, 583, 47); gc.strokeLine(570, 25, 580, 25);
        int seconds = (int) Math.ceil(world.getTimeRemaining());
        label(gc, world.getMode() == GameWorld.Mode.ENDLESS ? "∞" : String.format("00:%02d", seconds),
                600, 54, 32, seconds <= 10 ? Color.web("#ffb45e") : GOLD, false);
        panel(gc, 994, 16, 195, 47, false);
        label(gc, String.format("%,d m", (int) world.getDistanceMeters()), 1091, 49, 25, IVORY, true);
        panel(gc, PAUSE_X, PAUSE_Y, PAUSE_SIZE, PAUSE_SIZE, false);
        gc.setFill(IVORY); gc.fillRoundRect(1224, 29, 6, 25, 2, 2); gc.fillRoundRect(1238, 29, 6, 25, 2, 2);
        label(gc, String.format("SCORE  %,d", world.getScore()), 1187, 86, 13, IVORY, false, TextAlignment.RIGHT);

        if (world.getMode() == GameWorld.Mode.CATCH_THE_METRO) route(gc, world);
        label(gc, com.lastmetro.model.RouteTimeline.label(world.getRouteStage()), 640, 125, 12, IVORY, true);
        String status = world.getPlayer().isSlowed() ? "SLOWED"
                : world.getMagnetTime() > 0 ? "MAGNET " + (int) Math.ceil(world.getMagnetTime()) + "s"
                : world.getCombo() >= 3 ? world.getCombo() + " STREAK" : "";
        if (!status.isEmpty()) label(gc, status, 101, 107, 13, GOLD, false);
        if (world.getDodgeNoticeTime() > 0 && !world.isFinaleActive())
            label(gc, "DODGE +15", 1187, 106, 12, GOLD, false, TextAlignment.RIGHT);
        if (bannerTimer > 0 && !banner.isEmpty() && !world.isGameOver() && !world.isFinaleActive()) {
            gc.save(); gc.setGlobalAlpha(Math.min(1, bannerTimer * 2));
            panel(gc, 320, 650, 640, 37, false);
            label(gc, banner, 640, 674, 13, IVORY, true); gc.restore();
        }
        label(gc, jump.toUpperCase() + "  JUMP     " + slide.toUpperCase() + "  SLIDE", 27, 701, 11, IVORY, false);
        label(gc, "F8 SOUND " + (sound ? "ON" : "OFF") + "   F9 MOTION " + (lowMotion ? "LOW" : "FULL"),
                1253, 701, 10, IVORY, false, TextAlignment.RIGHT);
        gc.restore();
    }
    private void route(GraphicsContext gc, GameWorld world) {
        double x = 408, y = 91, width = 464;
        gc.setFill(Color.rgb(13, 22, 32, .8)); gc.fillRoundRect(x, y, width, 12, 6, 6);
        gc.setStroke(Color.web("#f3e2bd")); gc.setLineWidth(2); gc.strokeRoundRect(x, y, width, 12, 6, 6);
        double progress = world.getRouteProgress();
        gc.setFill(GOLD); gc.fillRoundRect(x + 2, y + 2, (width - 4) * progress, 8, 4, 4);
        double marker = x + 6 + (width - 12) * progress;
        gc.setFill(IVORY); gc.fillOval(marker - 4, y - 5, 8, 8);
        gc.setStroke(GOLD);
        for (double checkpoint : new double[] {RouteTimeline.FIRST_STREET_END, RouteTimeline.SECOND_STREET_END}) {
            double stop = x + width * checkpoint / GameWorld.NORMAL_GOAL_METERS;
            gc.strokeLine(stop, y - 8, stop, y + 10);
            gc.strokeRect(stop - 3, y - 13, 6, 6);
        }
        gc.setFill(Color.web("#263f4d")); gc.fillRoundRect(838, 65, 39, 25, 6, 6);
        gc.setStroke(IVORY); gc.strokeRoundRect(838, 65, 39, 25, 6, 6);
        gc.setFill(Color.web("#a4e1e6")); for (int i = 0; i < 3; i++) gc.fillRect(843 + i * 10, 70, 7, 10);
        gc.setFill(GOLD); gc.fillRect(841, 83, 33, 3);
        gc.setStroke(IVORY); gc.strokeLine(888, 63, 888, 101);
        for (int i = 0; i < 8; i++) { gc.setFill((i / 4 + i % 4) % 2 == 0 ? IVORY : Color.web("#253448")); gc.fillRect(890 + i % 4 * 4, 64 + i / 4 * 5, 4, 5); }
    }
    public static void panel(GraphicsContext gc, double x, double y, double w, double h, boolean gold) {
        gc.setFill(Color.rgb(7,12,23,.65)); gc.fillRect(x+3,y+5,w,h);
        gc.setFill(Color.web("#111c2a")); gc.fillRect(x,y,w,h);
        gc.setFill(gold?Color.web("#b28142"):Color.web("#293647"));gc.fillRect(x+2,y+2,w-4,h-4);
        gc.setFill(gold?Color.web("#f2d18a"):Color.web("#aab3ab"));gc.fillRect(x+2,y+2,w-4,2);gc.fillRect(x+2,y+2,2,h-4);
        gc.setFill(gold?Color.web("#76512d"):Color.web("#172332"));gc.fillRect(x+4,y+h-4,w-6,2);gc.fillRect(x+w-4,y+4,2,h-6);
    }
    private void heart(GraphicsContext gc,double x,double y,boolean full) {
        ArcadeArt.heart(gc,x,y,30,full);
    }
    public static void label(GraphicsContext gc, String text, double x, double y, int size, Color color, boolean center) {
        label(gc, text, x, y, size, color, center, center ? TextAlignment.CENTER : TextAlignment.LEFT);
    }
    private static void label(GraphicsContext gc, String text, double x, double y, int size, Color color, boolean center, TextAlignment align) {
        gc.setTextAlign(align); gc.setFont(Font.font(pixelFont, Math.max(7, Math.rint(size * .62))));
        gc.setFill(Color.rgb(4, 8, 15, .65)); gc.fillText(text, x + 1, y + 2, 620);
        gc.setFill(color); gc.fillText(text, x, y, 620); gc.setTextAlign(TextAlignment.LEFT);
    }
}
