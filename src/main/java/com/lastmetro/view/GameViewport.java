package com.lastmetro.view;

/** Letterboxed logical game space shared by drawing and pointer input. */
public final class GameViewport {
    private final double logicalWidth;
    private final double logicalHeight;
    private double viewWidth;
    private double viewHeight;
    private double scale;
    private double offsetX;
    private double offsetY;

    public GameViewport(double logicalWidth, double logicalHeight) {
        if (!Double.isFinite(logicalWidth) || logicalWidth <= 0.0
                || !Double.isFinite(logicalHeight) || logicalHeight <= 0.0) {
            throw new IllegalArgumentException("Logical dimensions must be finite and positive");
        }
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        resize(logicalWidth, logicalHeight);
    }

    public void resize(double width, double height) {
        if (!Double.isFinite(width) || width < 0.0 || !Double.isFinite(height) || height < 0.0) {
            throw new IllegalArgumentException("View dimensions must be finite and non-negative");
        }
        viewWidth = width;
        viewHeight = height;
        scale = width == 0.0 || height == 0.0
                ? 0.0
                : Math.min(width / logicalWidth, height / logicalHeight);
        offsetX = (width - logicalWidth * scale) / 2.0;
        offsetY = (height - logicalHeight * scale) / 2.0;
    }

    public boolean contains(double x, double y) {
        return scale > 0.0 && x >= offsetX && y >= offsetY
                && x < offsetX + logicalWidth * scale
                && y < offsetY + logicalHeight * scale;
    }

    public double toLogicalX(double x) { return (x - offsetX) / scale; }
    public double toLogicalY(double y) { return (y - offsetY) / scale; }
    public double getScale() { return scale; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public double getViewWidth() { return viewWidth; }
    public double getViewHeight() { return viewHeight; }
}
