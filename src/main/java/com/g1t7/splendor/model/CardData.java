package com.g1t7.splendor.model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ClassPathResource;

/**
 * Loads development cards from an external CSV file specified in
 * config.properties.
 * Falls back to a minimal hardcoded deck if the CSV cannot be read.
 *
 * Expected CSV format (each field quoted):
 * "ID,Tier,Points,Bonus,White,Blue,Green,Red,Black"
 * "1,1,0,WHITE,0,1,1,1,1"
 */
public class CardData {

    private CardData() {
    }

    /**
     * Build the full deck from the CSV path given in config.
     */
    public static List<Card> buildDeck(String csvPath) {
        List<Card> deck = new ArrayList<>();

        // Use ClassPathResource to read from the classpath (inside the JAR /
        // src/main/resources)
        try (InputStream is = new ClassPathResource(csvPath).getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                // Strip surrounding quotes from each line
                line = line.replace("\"", "").trim();
                if (line.isEmpty())
                    continue;
                if (!headerSkipped && line.startsWith("ID")) {
                    headerSkipped = true;
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 9)
                    continue;
                try {
                    // parts: ID, Tier, Points, Bonus, White, Blue, Green, Red, Black
                    int tier = Integer.parseInt(parts[1].trim());
                    int points = Integer.parseInt(parts[2].trim());
                    GemColor bonus = GemColor.valueOf(parts[3].trim().toUpperCase());
                    int white = Integer.parseInt(parts[4].trim());
                    int blue = Integer.parseInt(parts[5].trim());
                    int green = Integer.parseInt(parts[6].trim());
                    int red = Integer.parseInt(parts[7].trim());
                    int black = Integer.parseInt(parts[8].trim());
                    int[] cost = { white, blue, green, red, black, 0 };
                    deck.add(new Card(tier, bonus, points, cost));
                } catch (Exception e) {
                    System.err.println("[CardData] Skipping malformed line: " + line);
                }
            }
        } catch (IOException e) {
            // This catches the FileNotFoundException if the resource isn't found in the
            // classpath
            System.err.println("[CardData] CSV not found at classpath:" + csvPath + " - using fallback deck");
            return buildFallbackDeck();
        }

        if (deck.isEmpty())
            return buildFallbackDeck();

        System.out.println("[CardData] Loaded " + deck.size() + " cards from " + csvPath);
        return deck;
    }

    /** Convenience overload using default path. */
    public static List<Card> buildDeck() {
        return buildDeck("game/cards.csv");
    }

    /**
     * Minimal hardcoded deck (90 cards) used when the CSV is unavailable.
     */
    private static List<Card> buildFallbackDeck() {
        List<Card> d = new ArrayList<>();

        // tier, points, whiteCost, blueCost, greenCost, redCost, blackCost
        int[][] templates = {
                // Tier 1 (8 templates)
                { 1, 0, 0, 1, 1, 1, 1 },
                { 1, 0, 0, 2, 1, 0, 0 },
                { 1, 0, 0, 3, 0, 0, 0 },
                { 1, 0, 0, 2, 2, 0, 1 },
                { 1, 0, 3, 1, 0, 0, 1 },
                { 1, 0, 0, 0, 2, 0, 2 },
                { 1, 1, 0, 0, 4, 0, 0 },
                { 1, 1, 0, 1, 2, 1, 1 },

                // Tier 2 (6 templates)
                { 2, 1, 3, 2, 2, 0, 0 },
                { 2, 2, 0, 0, 0, 5, 0 },
                { 2, 2, 0, 0, 1, 4, 2 },
                { 2, 2, 2, 3, 0, 3, 0 },
                { 2, 3, 6, 0, 0, 0, 0 },
                { 2, 1, 2, 0, 4, 0, 1 },

                // Tier 3 (4 templates)
                { 3, 3, 0, 3, 3, 5, 3 },
                { 3, 4, 7, 0, 0, 0, 0 },
                { 3, 4, 6, 3, 3, 0, 0 },
                { 3, 5, 3, 0, 0, 0, 7 }
        };

        // generate the 90 cards
        for (int[] t : templates) {
            int tier = t[0];
            int points = t[1];

            // base costs from the template
            int w = t[2]; // White
            int u = t[3]; // Blue
            int g = t[4]; // Green
            int r = t[5]; // Red
            int b = t[6]; // Black

            d.add(new Card(tier, GemColor.WHITE, points, new int[] { w, u, g, r, b, 0 }));
            d.add(new Card(tier, GemColor.BLUE, points, new int[] { b, w, u, g, r, 0 }));
            d.add(new Card(tier, GemColor.GREEN, points, new int[] { r, b, w, u, g, 0 }));
            d.add(new Card(tier, GemColor.RED, points, new int[] { g, r, b, w, u, 0 }));
            d.add(new Card(tier, GemColor.BLACK, points, new int[] { u, g, r, b, w, 0 }));
        }

        return d;
    }
}