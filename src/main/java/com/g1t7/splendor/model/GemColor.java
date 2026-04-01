package com.g1t7.splendor.model;

/**
 * Gem colors used in the game, including gold.
 */
public enum GemColor {
    WHITE, BLUE, GREEN, RED, BLACK, GOLD;

    /**
     * Gets a color from its enum index.
     *
     * @param i ordinal index in enum declaration order
     * @return matching gem color
     */
    public static GemColor fromIndex(int i) {
        return values()[i];
    }
}