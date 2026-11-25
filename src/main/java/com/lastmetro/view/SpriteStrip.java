package com.lastmetro.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Image-backed animation frames loaded from a game resource. */
public final class SpriteStrip {
    private final Image image;
    private final int frameCount;
    private final List<Rectangle2D> frames;
    private final double referenceHeight;

    private SpriteStrip(Image image, List<Rectangle2D> frames) {
        this.image = image;
        this.frameCount = frames.size();
        this.frames = List.copyOf(frames);
        this.referenceHeight = frames.stream().mapToDouble(Rectangle2D::getHeight).max().orElse(1);
    }

    /** Loads equally-sized frames arranged in one horizontal strip. */
    public static Optional<SpriteStrip> load(Class<?> resourceOwner, String resourcePath, int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("frameCount must be positive");
        return loadImage(resourceOwner, resourcePath).flatMap(image -> {
            if (image.getWidth() % frameCount != 0) return Optional.empty();
            double frameWidth = image.getWidth() / frameCount;
            Rectangle2D[] frames = new Rectangle2D[frameCount];
            for (int frame = 0; frame < frameCount; frame++) {
                frames[frame] = new Rectangle2D(frame * frameWidth, 0, frameWidth, image.getHeight());
            }
            return Optional.of(new SpriteStrip(image, Arrays.asList(frames)));
        });
    }

    /** Loads animation frames at explicit source rectangles from a packed atlas. */
    public static Optional<SpriteStrip> loadFrames(Class<?> resourceOwner, String resourcePath,
                                                    Rectangle2D... frames) {
        if (frames == null || frames.length == 0) throw new IllegalArgumentException("frames are required");
        return loadImage(resourceOwner, resourcePath).flatMap(image -> {
            for (Rectangle2D frame : frames) {
                if (frame == null || frame.getWidth() <= 0 || frame.getHeight() <= 0
                        || frame.getMinX() < 0 || frame.getMinY() < 0
                        || frame.getMaxX() > image.getWidth() || frame.getMaxY() > image.getHeight()) {
                    return Optional.empty();
                }
            }
            return Optional.of(new SpriteStrip(image, Arrays.asList(frames)));
        });
    }

    /** Fixed atlas scale across a cycle, with every frame's feet anchored to the same ground. */
    public void drawGrounded(GraphicsContext gc, int frame, double centerX, double bottom, double height) {
        var source = frames.get(Math.max(0, Math.min(frameCount - 1, frame)));
        double scale = height / referenceHeight;
        double w = source.getWidth() * scale, h = source.getHeight() * scale;
        gc.drawImage(image, source.getMinX(), source.getMinY(), source.getWidth(), source.getHeight(),
                centerX - w / 2, bottom - h, w, h);
    }

    private static Optional<Image> loadImage(Class<?> resourceOwner, String resourcePath) {
        URL resource = resourceOwner.getResource(resourcePath);
        if (resource == null) return Optional.empty();
        try {
            Image image = new Image(resource.toExternalForm(), false);
            return image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0
                    ? Optional.empty() : Optional.of(image);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public void draw(GraphicsContext gc, int frame, double x, double y, double width, double height) {
        int safeFrame = Math.max(0, Math.min(frameCount - 1, frame));
        Rectangle2D source = frames.get(safeFrame);
        gc.drawImage(image, source.getMinX(), source.getMinY(), source.getWidth(), source.getHeight(),
                x, y, width, height);
    }

    /** Draws a frame within the target bounds without distorting its original proportions. */
    public void drawFitted(GraphicsContext gc, int frame, double x, double y, double width, double height) {
        int safeFrame = Math.max(0, Math.min(frameCount - 1, frame));
        Rectangle2D source = frames.get(safeFrame);
        double scale = Math.min(width / source.getWidth(), height / source.getHeight());
        double drawWidth = source.getWidth() * scale;
        double drawHeight = source.getHeight() * scale;
        gc.drawImage(image, source.getMinX(), source.getMinY(), source.getWidth(), source.getHeight(),
                x + (width - drawWidth) / 2.0, y + (height - drawHeight) / 2.0, drawWidth, drawHeight);
    }

    public void drawFittedMirrored(GraphicsContext gc, int frame, double x, double y,
                                   double width, double height) {
        int safeFrame = Math.max(0, Math.min(frameCount - 1, frame));
        Rectangle2D source = frames.get(safeFrame);
        double scale = Math.min(width / source.getWidth(), height / source.getHeight());
        double drawWidth = source.getWidth() * scale;
        double drawHeight = source.getHeight() * scale;
        double drawX = x + (width - drawWidth) / 2.0;
        double drawY = y + (height - drawHeight) / 2.0;
        gc.drawImage(image, source.getMinX(), source.getMinY(), source.getWidth(), source.getHeight(),
                drawX + drawWidth, drawY, -drawWidth, drawHeight);
    }

    public int getFrameCount() { return frameCount; }
}
