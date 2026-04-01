package com.g1t7.splendor.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.g1t7.splendor.model.Card;
import com.g1t7.splendor.model.GemColor;

/**
 * Loads development cards from CSV.
 * Uses a fallback deck if the file cannot be read.
 */
public class CardData {

    private static final Logger logger = LoggerFactory.getLogger(CardData.class);
    private static final int EXPECTED_CSV_COLUMNS = 9;

    private CardData() {
    }

    /**
     * Builds the full deck from the configured CSV path.
     *
     * @param csvPath classpath location of the CSV file
     * @return list of cards
     */
    public static List<Card> buildDeck(String csvPath) {
        List<Card> deck = new ArrayList<>();

        try (InputStream is = new ClassPathResource(csvPath).getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                String[] parts = line.split(",");
                if (parts.length < EXPECTED_CSV_COLUMNS)
                    continue;

                try {
                    int tier = Integer.parseInt(parts[1].trim());
                    int points = Integer.parseInt(parts[2].trim());
                    GemColor bonus = GemColor.valueOf(parts[3].trim().toUpperCase());

                    int white = Integer.parseInt(parts[4].trim());
                    int blue = Integer.parseInt(parts[5].trim());
                    int green = Integer.parseInt(parts[6].trim());
                    int red = Integer.parseInt(parts[7].trim());
                    int black = Integer.parseInt(parts[8].trim());

                    int[] cost = { white, blue, green, red, black, 0 }; // 0 for Gold
                    deck.add(new Card(tier, bonus, points, cost));
                } catch (Exception e) {
                    logger.warn("Skipping malformed line in Cards CSV: {}", line);
                }
            }
        } catch (IOException e) {
            logger.error("CSV not found at classpath: {} - using fallback deck", csvPath);
            return buildFallbackDeck();
        }

        if (deck.isEmpty())
            return buildFallbackDeck();

        logger.info("Loaded {} cards from {}", deck.size(), csvPath);
        return deck;
    }

    /**
     * Builds a fallback deck used when CSV loading fails.
     */
    private static List<Card> buildFallbackDeck() {
        List<Card> fallbackDeck = new ArrayList<>();
        int[][] templates = {
                { 1, 0, 0, 1, 1, 1, 1 }, { 1, 0, 0, 2, 1, 0, 0 }, { 1, 0, 0, 3, 0, 0, 0 },
                { 1, 0, 0, 2, 2, 0, 1 }, { 1, 0, 3, 1, 0, 0, 1 }, { 1, 0, 0, 0, 2, 0, 2 },
                { 1, 1, 0, 0, 4, 0, 0 }, { 1, 1, 0, 1, 2, 1, 1 }, { 2, 1, 3, 2, 2, 0, 0 },
                { 2, 2, 0, 0, 0, 5, 0 }, { 2, 2, 0, 0, 1, 4, 2 }, { 2, 2, 2, 3, 0, 3, 0 },
                { 2, 3, 6, 0, 0, 0, 0 }, { 2, 1, 2, 0, 4, 0, 1 }, { 3, 3, 0, 3, 3, 5, 3 },
                { 3, 4, 7, 0, 0, 0, 0 }, { 3, 4, 6, 3, 3, 0, 0 }, { 3, 5, 3, 0, 0, 0, 7 }
        };

        for (int[] t : templates) {
            int tier = t[0], points = t[1];
            int w = t[2], u = t[3], g = t[4], r = t[5], b = t[6];

            fallbackDeck.add(new Card(tier, GemColor.WHITE, points, new int[] { w, u, g, r, b, 0 }));
            fallbackDeck.add(new Card(tier, GemColor.BLUE, points, new int[] { b, w, u, g, r, 0 }));
            fallbackDeck.add(new Card(tier, GemColor.GREEN, points, new int[] { r, b, w, u, g, 0 }));
            fallbackDeck.add(new Card(tier, GemColor.RED, points, new int[] { g, r, b, w, u, 0 }));
            fallbackDeck.add(new Card(tier, GemColor.BLACK, points, new int[] { u, g, r, b, w, 0 }));
        }
        return fallbackDeck;
    }
}