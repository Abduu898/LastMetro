package com.lastmetro.view;

import com.lastmetro.model.GameWorld;
import com.lastmetro.model.RouteTimeline;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Authored route sheets: independent layers and finite, time-matched ending placement. */
public final class PixelRouteScenery {
    private Image far, buildings, walk, tunnel, wall, front, transfer, ending, crown;
    public void load() {
        far = load("street-far.png"); buildings = load("street-buildings.png"); walk = load("street-walkway.png");
        tunnel = load("underground-far.png"); wall = load("underground-wall.png"); front = load("underground-front.png");
        transfer = load("transfer.png"); ending = load("platform-ending.png");
        crown = load("leaf-crown.png");
    }
    private Image load(String name) {
        var url = getClass().getResource("/assets/pixel-route/" + name);
        if (url == null) return null;
        var image = new Image(url.toExternalForm()); return image.isError() ? null : image;
    }
    public boolean isReady() { return far != null && buildings != null && walk != null && tunnel != null && wall != null && front != null; }
    public boolean hasEnding() { return ending != null; }

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
            // This entrance is one world object, placed to reach the runner at the section boundary.
            double scale = 335 / walk.getHeight();
            double entranceX = (1 - progress) * 13 * GameWorld.BASE_SCROLL_SPEED + 150 - 170 * scale;
            gc.drawImage(walk, 2180, 0, walk.getWidth() - 2180, walk.getHeight(), snap(entranceX), 385,
                    (walk.getWidth() - 2180) * scale, 335);
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
        if (world != null && ending != null && (world.isFinaleActive() || world.getRouteProgress() > .97)) {
            double alpha = world.isFinaleActive() ? 1 : Math.min(1, (world.getRouteProgress() - .97) / .03);
            gc.setGlobalAlpha(alpha);
            // Preserve the wide strip's aspect ratio: stretching it to 16:9 makes NPCs and trains too tall.
            double scale=.76, top=576-448*scale;
            gc.setFill(Color.web("#192023"));gc.fillRect(0,0,1280,720);
            loop(gc,tunnel,tunnel.getWidth(),0,180,0,Math.ceil(top),0);
            gc.drawImage(ending,-300,snap(top),ending.getWidth()*scale,ending.getHeight()*scale);
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
