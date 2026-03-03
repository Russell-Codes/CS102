package com.g1t7.splendor.model;

import java.io.Serializable;

public class Noble implements Serializable {


    private int victoryPoints = 3;
    // requirement[i] = cards of GemColor.fromIndex(i) needed (WHITE..BLACK, indices 0-4)
    private int[] requirement = new int[5];

    public Noble(int white, int blue, int green, int red, int black) {
        requirement[0] = white;
        requirement[1] = blue;
        requirement[2] = green;
        requirement[3] = red;
        requirement[4] = black;
    }

    /**
     * Returns true if the player's card bonuses satisfy this noble's requirements.
     * @param cardBonuses int[5] indexed by GemColor ordinal (WHITE=0 .. BLACK=4)
     */
    public boolean isSatisfiedBy(int[] cardBonuses) {
        for (int i = 0; i < 5; i++) {
            if (cardBonuses[i] < requirement[i]) return false;
        }
        return true;
    }

    public int getVictoryPoints() { return victoryPoints; }
    public int[] getRequirement() { return requirement; }
}
