package com.lastmetro.patterns.factory;

import java.util.Random;

/**
 * Chooses a route pattern from the current section without repeating a draw.
 */
public class HazardFactory {
    private final Random random;

    public HazardFactory() {
        this.random = new Random();
    }

    public HazardFactory(Random random) {
        this.random = random;
    }

    public enum Pattern { TOKENS, BARRIER, CRATE, SCOOTER, SKID, TRANSFER, PIGEON, PLATFORM, RECOVERY, EMPTY }

    public Pattern choosePattern(int stage, boolean opening, Pattern previous) {
        if (opening) return switch (stage) {
            case 0 -> Pattern.TOKENS;
            case 1 -> Pattern.TRANSFER;
            default -> Pattern.PLATFORM;
        };
        Pattern[] choices = switch (stage) {
            case 0 -> new Pattern[] {Pattern.BARRIER, Pattern.CRATE, Pattern.SCOOTER, Pattern.SKID, Pattern.RECOVERY};
            case 1 -> new Pattern[] {Pattern.TRANSFER, Pattern.PIGEON, Pattern.BARRIER, Pattern.RECOVERY};
            default -> new Pattern[] {Pattern.PLATFORM, Pattern.PIGEON, Pattern.TRANSFER, Pattern.RECOVERY};
        };
        int index = random.nextInt(choices.length);
        if (choices[index] == previous) index = (index + 1) % choices.length;
        return choices[index];
    }
}
