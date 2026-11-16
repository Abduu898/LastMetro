package com.lastmetro.model;

/**
 * Base abstract class for all physical entities in the game world.
 */
public abstract class Entity {
    protected double x;
    protected double y;
    protected double width;
    protected double height;

    public Entity(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void update(double deltaTime);

    /**
     * AABB (Axis-Aligned Bounding Box) collision check.
     */
    public boolean intersects(Entity other) {
        return this.x < other.x + other.width &&
                this.x + this.width > other.x &&
                this.y < other.y + other.height &&
                this.y + this.height > other.y;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setHeight(double height) { this.height = height; }
}