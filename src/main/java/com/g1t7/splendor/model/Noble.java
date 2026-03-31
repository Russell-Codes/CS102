package com.g1t7.splendor.model;

import java.io.Serializable;

/**
 * Represents a Noble tile in the game.
 * Nobles visit players automatically when specific card bonus requirements are
 * met.
 */
public class Noble implements Serializable {

    private int victoryPoints = 3;

    // requirement[i] = cards of GemColor.fromIndex(i) needed (WHITE..BLACK, indices
    // 0-4)
    private int[] requirement = new int[Player.REGULAR_GEM_TYPES];

    public Noble(int white, int blue, int green, int red, int black) {
        requirement[GemColor.WHITE.ordinal()] = white;
        requirement[GemColor.BLUE.ordinal()] = blue;
        requirement[GemColor.GREEN.ordinal()] = green;
        requirement[GemColor.RED.ordinal()] = red;
        requirement[GemColor.BLACK.ordinal()] = black;
    }

    /**
     * Evaluates if the player's accumulated card bonuses satisfy this noble's
     * requirements.
     * * @param cardBonuses Array of current gem bonuses held by the player.
     * 
     * @return true if the player meets or exceeds all gem requirements.
     */
    public boolean isSatisfiedBy(int[] cardBonuses) {
        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            if (cardBonuses[i] < requirement[i]) {
                return false;
            }
        }
        return true;
    }

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public int[] getRequirement() {
        return requirement;
    }
}