package com.lastmetro.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.net.URL;
import java.util.Optional;

/** A horizontal, equally-sized sprite strip loaded from the game's resources. */
public final class SpriteStrip {
    private final Image image;
    private final int frameCount;
    private final double frameWidth;
    private final double frameHeight;

    private SpriteStrip(Image image, int frameCount) {
        this.image = image;
        this.frameCount = frameCount;
        this.frameWidth = image.getWidth() / frameCount;
        this.frameHeight = image.getHeight();
    }

    /** Loads a resource such as {@code /assets/sprites/commuter-run.png}; absent art is expected. */
    public static Optional<SpriteStrip> load(Class<?> resourceOwner, String resourcePath, int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("frameCount must be positive");
        URL resource = resourceOwner.getResource(resourcePath);
        if (resource == null) return Optional.empty();
        try {
            Image image = new Image(resource.toExternalForm(), false);
            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0
                    || image.getWidth() % frameCount != 0) {
                return Optional.empty();
            }
            return Optional.of(new SpriteStrip(image, frameCount));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public void draw(GraphicsContext gc, int frame, double x, double y, double width, double height) {
        int safeFrame = Math.max(0, Math.min(frameCount - 1, frame));
        gc.drawImage(image, safeFrame * frameWidth, 0, frameWidth, frameHeight, x, y, width, height);
    }

    public int getFrameCount() { return frameCount; }
}
