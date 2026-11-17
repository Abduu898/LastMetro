package com.lastmetro.patterns.observer;

/**
 * Observer Pattern interface decoupling game events from the JavaFX UI.
 */
public interface GameObserver {
    void onSarcasticEvent(String message);
    void onFinaleTriggered(String reason);
}