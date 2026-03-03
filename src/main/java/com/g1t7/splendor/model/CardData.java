package com.g1t7.splendor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Static card definitions for the Splendor deck (25 cards across 3 tiers).
 * Cost array: new int[]{white, blue, green, red, black, 0}
 * GemColor ordinals: WHITE=0, BLUE=1, GREEN=2, RED=3, BLACK=4, GOLD=5
 */
public class CardData {

    private CardData() {}

    public static List<Card> buildDeck() {
        List<Card> deck = new ArrayList<>();

        // ----- TIER 1 (10 cards, 0-1 VP) -----
        // Format: tier, gemColor (bonus), VP, {W, Bl, Gr, R, Bk, 0}
        deck.add(new Card(1, GemColor.WHITE,  0, new int[]{0, 1, 1, 1, 1, 0}));
        deck.add(new Card(1, GemColor.WHITE,  0, new int[]{0, 0, 2, 0, 2, 0}));
        deck.add(new Card(1, GemColor.BLUE,   0, new int[]{1, 0, 1, 1, 1, 0}));
        deck.add(new Card(1, GemColor.BLUE,   0, new int[]{0, 0, 0, 2, 2, 0}));
        deck.add(new Card(1, GemColor.GREEN,  0, new int[]{1, 1, 0, 1, 1, 0}));
        deck.add(new Card(1, GemColor.GREEN,  0, new int[]{0, 2, 0, 0, 2, 0}));
        deck.add(new Card(1, GemColor.RED,    0, new int[]{1, 1, 1, 0, 1, 0}));
        deck.add(new Card(1, GemColor.RED,    0, new int[]{2, 0, 0, 0, 2, 0}));
        deck.add(new Card(1, GemColor.BLACK,  0, new int[]{1, 1, 1, 1, 0, 0}));
        deck.add(new Card(1, GemColor.BLACK,  1, new int[]{0, 0, 0, 4, 0, 0}));

        // ----- TIER 2 (10 cards, 1-3 VP) -----
        deck.add(new Card(2, GemColor.WHITE,  1, new int[]{0, 0, 3, 2, 2, 0}));
        deck.add(new Card(2, GemColor.WHITE,  2, new int[]{0, 0, 0, 5, 0, 0}));
        deck.add(new Card(2, GemColor.BLUE,   1, new int[]{0, 0, 2, 3, 2, 0}));
        deck.add(new Card(2, GemColor.BLUE,   2, new int[]{0, 0, 0, 0, 5, 0}));
        deck.add(new Card(2, GemColor.GREEN,  1, new int[]{2, 3, 0, 0, 2, 0}));
        deck.add(new Card(2, GemColor.GREEN,  2, new int[]{0, 5, 0, 0, 0, 0}));
        deck.add(new Card(2, GemColor.RED,    1, new int[]{2, 2, 0, 0, 3, 0}));
        deck.add(new Card(2, GemColor.RED,    2, new int[]{5, 0, 0, 0, 0, 0}));
        deck.add(new Card(2, GemColor.BLACK,  1, new int[]{3, 0, 2, 0, 2, 0}));  // corrected
        deck.add(new Card(2, GemColor.BLACK,  3, new int[]{0, 0, 0, 3, 3, 0}));

        // ----- TIER 3 (5 cards, 3-5 VP) -----
        deck.add(new Card(3, GemColor.WHITE,  4, new int[]{3, 0, 0, 3, 6, 0}));
        deck.add(new Card(3, GemColor.BLUE,   4, new int[]{0, 0, 3, 6, 3, 0}));
        deck.add(new Card(3, GemColor.GREEN,  4, new int[]{3, 6, 0, 0, 3, 0}));
        deck.add(new Card(3, GemColor.RED,    4, new int[]{0, 3, 6, 3, 0, 0}));
        deck.add(new Card(3, GemColor.BLACK,  5, new int[]{0, 0, 0, 7, 0, 0}));

        return deck;
    }
}
