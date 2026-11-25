package com.lastmetro.view;

import com.lastmetro.model.GameWorld;
import com.lastmetro.model.RouteTimeline;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Authored route sheets: independent layers and distance-based entrances and an animated train. */
public final class PixelRouteScenery {
    private Image far, buildings, walk, tunnel, wall, front, transfer, train, entrance, crown;
    public void load() {
        far = load("street-far.png"); buildings = load("street-buildings.png"); walk = load("street-walkway.png");
        tunnel = load("underground-far.png"); wall = load("underground-wall.png"); front = load("underground-front.png");
        transfer = load("transfer.png"); train = load("metro-car.png"); entrance = load("metro-entrance.png");
        crown = load("leaf-crown.png");
    }
    private Image load(String name) {
        var url = getClass().getResource("/assets/pixel-route/" + name);
        if (url == null) return null;
        var image = new Image(url.toExternalForm()); return image.isError() ? null : image;
    }
    public boolean isReady() { return far != null && buildings != null && walk != null && tunnel != null && wall != null && front != null; }
    public boolean hasEnding() { return train != null; }

    public void continuation(GraphicsContext gc, GameWorld world, double menuTime) {
        boolean destination = world != null && world.isTransferring() && world.getTransferProgress() >= .5;
        int stage = world == null ? 1 : world.getRouteStage() + (destination ? 1 : 0);
        double progress = world == null ? .22 + Math.sin(menuTime * .03) * .08 : destination ? 0 : world.getStageProgress();
        gc.save(); gc.setImageSmoothing(false);
        if (stage < 2) {
            double camera = world == null ? progress * 13 * GameWorld.BASE_SCROLL_SPEED : destination ? 0 : world.getStageDistance() * 40;
            gc.setFill(Color.web("#6a617c")); gc.fillRect(0, 0, 1280, 720);
            loop(gc, far, far.getWidth(), 0, far.getHeight(), 0, 350, camera * .08);
            // Buildings end at the back curb; the playable lane is below them.
            loop(gc, buildings, 2100, 0, buildings.getHeight(), 116, 428, camera * .35);
            treeCrowns(gc, camera);
            loop(gc, walk, 2100, 0, walk.getHeight(), 385, 335, camera);
        } else {
            double camera = world == null ? progress * 12 * GameWorld.BASE_SCROLL_SPEED : destination ? 0 : world.getStageDistance() * 40;
            gc.setFill(Color.web("#1b2023")); gc.fillRect(0, 0, 1280, 720);
            loop(gc, tunnel, tunnel.getWidth(), 0, tunnel.getHeight(), 0, 480, camera * .08);
            loop(gc, wall, 2200, 0, wall.getHeight(), 180, 365, camera * .35);
            // The train portion is reserved for the ending; only the loopable platform is used here.
            double width = 1850 * .8, offset = camera % width;
            for (double x = -snap(offset); x < 1280; x += width) {
                gc.drawImage(front, 0, 0, 1850, 220, x, 400, width, 176);
                gc.drawImage(front, 0, 220, 1850, front.getHeight() - 220, x, 576, width, 144);
            }
        }
        gc.restore();
    }

    public void drawEntrance(GraphicsContext gc, GameWorld world) {
        int stage = world.getRouteStage();
        if (stage >= 2 || world.getTransferProgress() >= .5) return;
        double remaining = RouteTimeline.end(stage) - world.getDistanceMeters();
        if (remaining > 31) return;
        double center = snap(175 + remaining * 40);
        gc.save();
        gc.setImageSmoothing(false);
        if (stage == 0 && entrance != null) {
            gc.drawImage(entrance, center - 184, 354, 310, 273);
            for (double post : new double[] {center - 107, center + 101}) {
                gc.setFill(Color.web("#23362d")); gc.fillRect(post - 6, 345, 12, 14);
                gc.fillRect(post - 9, 341, 18, 6);
                gc.setFill(Color.web("#d9954f")); gc.fillRect(post - 5, 347, 10, 7);
                gc.setFill(Color.web("#ffdb86")); gc.fillRect(post - 3, 348, 5, 4);
            }
        } else if (stage == 1 && wall != null) {
            gc.setFill(Color.web("#202a2e"));
            gc.fillRect(center - 102, 372, 204, 204);
            gc.drawImage(wall, 2279, 0, 344, 348, center - 102, 370, 204, 206);
            ArcadeHud.panel(gc, center - 82, 341, 164, 32, false);
            ArcadeHud.label(gc, "TO THE PLATFORM", center, 362, 14, Color.web("#ffdf91"), true);
        }
        gc.restore();
    }

    public void drawTrain(GraphicsContext gc, GameWorld world, boolean reducedMotion) {
        if (train == null || world.getMode() != GameWorld.Mode.CATCH_THE_METRO || world.getRouteStage() != 2) return;
        double remaining = GameWorld.NORMAL_GOAL_METERS - world.getDistanceMeters();
        if (!world.isFinaleActive() && remaining > 24) return;
        double x = world.isFinaleActive() ? world.getMetroTrainX()
                : GameWorld.TRAIN_STOP_X + 640 * Math.pow(remaining / 24, 1.5);
        if (reducedMotion && !world.isFinaleActive()) x = GameWorld.TRAIN_STOP_X;
        double scale = .93, y = 358;
        gc.save();
        gc.setImageSmoothing(false);
        gc.translate(snap(x), y);
        gc.scale(scale, scale);
        gc.drawImage(train, 0, 0);
        // The supplied closed door leaves slide into the jambs over a lit interior.
        double leaf = 53, left = 154, top = 58, height = 176;
        double visibleLeaf = leaf * world.getMetroDoorProgress();
        gc.setFill(Color.web("#162b29")); gc.fillRect(left, top, 2 * leaf, height);
        gc.setFill(Color.web("#334d43")); gc.fillRect(left + 10, top + 12, 2 * leaf - 20, 130);
        gc.setFill(Color.web("#73958a")); gc.fillRect(left + 22, top + 27, 2 * leaf - 44, 42);
        gc.setFill(Color.web("#aab6a0")); gc.fillRect(left + 24, top + 31, 2 * leaf - 48, 5);
        gc.setFill(Color.web("#263833")); gc.fillRect(left + 16, top + 89, 2 * leaf - 32, 26);
        gc.setFill(Color.web("#516e60")); gc.fillRect(left + 14, top + 112, 2 * leaf - 28, 10);
        gc.setFill(Color.web("#b79a5e"));
        gc.fillRect(left + 9, top + 12, 3, 134);
        gc.fillRect(left + 2 * leaf - 12, top + 12, 3, 134);
        gc.setFill(Color.web("#edca83")); gc.fillRect(left + 2, top + 3, 2 * leaf - 4, 7);
        gc.setFill(Color.web("#625640")); gc.fillRect(left + 2, top + 140, 2 * leaf - 4, 34);
        gc.setFill(Color.web("#94a99a")); gc.fillRect(left, top + height - 5, 2 * leaf, 5);
        if (visibleLeaf > 1) {
            gc.drawImage(train, left + leaf - visibleLeaf, top, visibleLeaf, height,
                    left, top, visibleLeaf, height);
            gc.drawImage(train, left + leaf, top, visibleLeaf, height,
                    left + 2 * leaf - visibleLeaf, top, visibleLeaf, height);
        }
        gc.restore();
    }

    private void treeCrowns(GraphicsContext gc, double camera) {
        if(crown==null)return;
        // Reuse the supplied leaf pixels to finish crowns cut by the sheet's top edge.
        double scale=335/walk.getHeight(),width=2100*scale,offset=camera%width;
        for(double tile=-offset;tile<1280;tile+=width)for(int sourceX:new int[]{611,1896}) {
            double center=tile+sourceX*scale;
            if(center < -140 || center > 1420)continue;
            gc.save();gc.translate(snap(center-132*scale),385);gc.scale(1,-1);
            gc.drawImage(crown,0,0,264,82,0,0,264*scale,82*scale);gc.restore();
        }
    }

    private void loop(GraphicsContext gc, Image image, double sw, double sy, double sh, double y, double height, double camera) {
        double width = sw / sh * height, offset = camera % width;
        for (double x = -snap(offset); x < 1280; x += width)
            gc.drawImage(image, 0, sy, sw, sh, x, y, Math.ceil(width) + 1, height);
    }

    public void transfer(GraphicsContext gc, GameWorld world, String font) {
        if (!world.isTransferring() || transfer == null) return;
        double progress = world.getTransferProgress();
        double fade = Math.min(1, Math.min(progress / .16, (1 - progress) / .16));
        fade = fade * fade * (3 - 2 * fade);
        gc.save(); gc.setGlobalAlpha(fade); gc.setFill(Color.web("#10141c")); gc.fillRect(0, 0, 1280, 720);
        gc.setImageSmoothing(false); gc.drawImage(transfer, 0, 194, 1280, 298);
        gc.setFill(Color.web("#fff0c8")); gc.setFont(javafx.scene.text.Font.font(font, 15));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("CORRESPONDANCE", 640, 142);
        gc.setFont(javafx.scene.text.Font.font(font, 9));
        gc.fillText(RouteTimeline.label(world.getRouteStage() + 1), 640, 551);
        gc.fillText("LE CHRONO EST EN PAUSE", 640, 585);
        gc.setFill(Color.web("#243c35")); gc.fillRect(390, 615, 500, 10);
        gc.setFill(Color.web("#a9d286")); gc.fillRect(390, 615, 500 * progress, 10);
        gc.restore();
    }
    private double snap(double x) { return Math.rint(x / 2) * 2; }
}
