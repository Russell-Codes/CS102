package com.g1t7.splendor.model;

<<<<<<< HEAD
import java.io.*;
=======
>>>>>>> remotes/origin/init
import java.util.ArrayList;
import java.util.List;

/**
<<<<<<< HEAD
 * Loads development cards from an external CSV file specified in config.properties.
 * Falls back to a minimal hardcoded deck if the CSV cannot be read.
 *
 * Expected CSV format (each field quoted):
 *   "ID,Tier,Points,Bonus,White,Blue,Green,Red,Black"
 *   "1,1,0,WHITE,0,1,1,1,1"
=======
 * Static card definitions for the Splendor deck (25 cards across 3 tiers).
 * Cost array: new int[]{white, blue, green, red, black, 0}
 * GemColor ordinals: WHITE=0, BLUE=1, GREEN=2, RED=3, BLACK=4, GOLD=5
>>>>>>> remotes/origin/init
 */
public class CardData {

    private CardData() {}

<<<<<<< HEAD
    /**
     * Build the full deck from the CSV path given in config.
     */
    public static List<Card> buildDeck(String csvPath) {
        List<Card> deck = new ArrayList<>();
        File file = new File(csvPath);
        if (!file.exists()) {
            System.err.println("[CardData] CSV not found at " + csvPath + " – using fallback deck");
            return buildFallbackDeck();
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                // Strip surrounding quotes from each line
                line = line.replace("\"", "").trim();
                if (line.isEmpty()) continue;
                if (!headerSkipped && line.startsWith("ID")) {
                    headerSkipped = true;
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 9) continue;
                try {
                    // parts: ID, Tier, Points, Bonus, White, Blue, Green, Red, Black
                    int tier   = Integer.parseInt(parts[1].trim());
                    int points = Integer.parseInt(parts[2].trim());
                    GemColor bonus = GemColor.valueOf(parts[3].trim().toUpperCase());
                    int white  = Integer.parseInt(parts[4].trim());
                    int blue   = Integer.parseInt(parts[5].trim());
                    int green  = Integer.parseInt(parts[6].trim());
                    int red    = Integer.parseInt(parts[7].trim());
                    int black  = Integer.parseInt(parts[8].trim());
                    int[] cost = {white, blue, green, red, black, 0};
                    deck.add(new Card(tier, bonus, points, cost));
                } catch (Exception e) {
                    System.err.println("[CardData] Skipping malformed line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("[CardData] Error reading CSV: " + e.getMessage());
            return buildFallbackDeck();
        }
        if (deck.isEmpty()) return buildFallbackDeck();
        System.out.println("[CardData] Loaded " + deck.size() + " cards from " + csvPath);
        return deck;
    }

    /** Convenience overload using default path. */
    public static List<Card> buildDeck() {
        return buildDeck("resources/cards.csv");
    }

    /**
     * Minimal hardcoded deck (25 cards) used when the CSV is unavailable.
     */
    private static List<Card> buildFallbackDeck() {
        List<Card> d = new ArrayList<>();
        d.add(new Card(1, GemColor.WHITE,0,new int[]{0,1,1,1,1,0}));
        d.add(new Card(1, GemColor.WHITE,0,new int[]{0,0,2,0,2,0}));
        d.add(new Card(1, GemColor.BLUE, 0,new int[]{1,0,1,1,1,0}));
        d.add(new Card(1, GemColor.BLUE, 0,new int[]{0,0,0,2,2,0}));
        d.add(new Card(1, GemColor.GREEN,0,new int[]{1,1,0,1,1,0}));
        d.add(new Card(1, GemColor.GREEN,0,new int[]{0,2,0,0,2,0}));
        d.add(new Card(1, GemColor.RED,  0,new int[]{1,1,1,0,1,0}));
        d.add(new Card(1, GemColor.RED,  0,new int[]{2,0,0,0,2,0}));
        d.add(new Card(1, GemColor.BLACK,0,new int[]{1,1,1,1,0,0}));
        d.add(new Card(1, GemColor.BLACK,1,new int[]{0,0,0,4,0,0}));
        d.add(new Card(2, GemColor.WHITE,1,new int[]{0,0,3,2,2,0}));
        d.add(new Card(2, GemColor.WHITE,2,new int[]{0,0,0,5,0,0}));
        d.add(new Card(2, GemColor.BLUE, 1,new int[]{0,0,2,3,2,0}));
        d.add(new Card(2, GemColor.BLUE, 2,new int[]{0,0,0,0,5,0}));
        d.add(new Card(2, GemColor.GREEN,1,new int[]{2,3,0,0,2,0}));
        d.add(new Card(2, GemColor.GREEN,2,new int[]{0,5,0,0,0,0}));
        d.add(new Card(2, GemColor.RED,  1,new int[]{2,2,0,0,3,0}));
        d.add(new Card(2, GemColor.RED,  2,new int[]{5,0,0,0,0,0}));
        d.add(new Card(2, GemColor.BLACK,1,new int[]{3,0,2,0,2,0}));
        d.add(new Card(2, GemColor.BLACK,3,new int[]{0,0,0,3,3,0}));
        d.add(new Card(3, GemColor.WHITE,4,new int[]{3,0,0,3,6,0}));
        d.add(new Card(3, GemColor.BLUE, 4,new int[]{0,0,3,6,3,0}));
        d.add(new Card(3, GemColor.GREEN,4,new int[]{3,6,0,0,3,0}));
        d.add(new Card(3, GemColor.RED,  4,new int[]{0,3,6,3,0,0}));
        d.add(new Card(3, GemColor.BLACK,5,new int[]{0,0,0,7,0,0}));
        return d;
    }
=======
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
>>>>>>> remotes/origin/init
}
