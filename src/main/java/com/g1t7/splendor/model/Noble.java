package com.g1t7.splendor.model;

/**
 * Represents a Noble tile in the game.
 * Nobles visit players automatically when specific card bonus requirements are
 * met.
 */
public class Noble {

    private int victoryPoints = 3;
    private int imageId; // for frontend display, corresponds to index in GameConfig.nobles list
    // requirement[i] = cards of GemColor.fromIndex(i) needed (WHITE..BLACK, indices
    // 0-4)
    private int[] requirement = new int[Player.REGULAR_GEM_TYPES];

    public Noble(int imageId, int white, int blue, int green, int red, int black) {
        this.imageId = imageId;
        requirement[GemColor.WHITE.ordinal()] = white;
        requirement[GemColor.BLUE.ordinal()] = blue;
        requirement[GemColor.GREEN.ordinal()] = green;
        requirement[GemColor.RED.ordinal()] = red;
        requirement[GemColor.BLACK.ordinal()] = black;
    }

    /**
     * Checks whether a player's bonuses satisfy this noble.
     *
     * @param cardBonuses current bonus counts by color
     * @return true if all requirements are met
     */
    public boolean isSatisfiedBy(int[] cardBonuses) {
        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            if (cardBonuses[i] < requirement[i]) {
                return false;
            }
        }
        return true;
    }

    public int getImageId() {
        return imageId;
    }

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public int[] getRequirement() {
        return requirement;
    }
}