package com.lastmetro.model;

/** Active-play timing; transfer animations do not consume the departure countdown. */
public final class RouteTimeline {
    public static final double FIRST_STREET_END = 20;
    public static final double SECOND_STREET_END = 33;
    public static final double TRANSFER_SECONDS = 2.4;
    private RouteTimeline() { }
    public static double start(int stage) { return stage == 0 ? 0 : stage == 1 ? FIRST_STREET_END : SECOND_STREET_END; }
    public static double end(int stage) { return stage == 0 ? FIRST_STREET_END : stage == 1 ? SECOND_STREET_END : GameWorld.INITIAL_TIME; }
    public static String label(int stage) { return stage == 0 ? "01  RUES DE PARIS" : stage == 1 ? "02  ACCÈS MÉTRO" : "03  QUAI SOUTERRAIN"; }
}
