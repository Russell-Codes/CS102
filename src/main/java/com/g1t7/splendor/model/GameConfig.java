package com.g1t7.splendor.model;

import java.io.*;
import java.util.Properties;

/**
 * Reads game parameters from resources/config.properties.
 * Falls back to Splendor default values when a key is missing.
 */
public class GameConfig implements Serializable {

    private static final String CONFIG_PATH = "game/config.properties";

    private int winScore = 15;
    private String cardFile = "game/cards.csv";

    // Gem counts by number of players (index 0 unused, 2–4 used)
    private int gems2Players = 4;
    private int gems3Players = 5;
    private int gems4Players = 7;

    // Noble counts by number of players
    private int nobles2Players = 3;
    private int nobles3Players = 4;
    private int nobles4Players = 5;

    // Gold coins (always 5 in standard Splendor)
    private int goldCoins = 5;

    public GameConfig() {
        load();
    }

    private void load() {
        Properties props = new Properties();
        // Try file-system path first, then classpath
        File file = new File(CONFIG_PATH);
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                props.load(is);
            } catch (IOException ignored) {
            }
        } else {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (is != null)
                    props.load(is);
            } catch (IOException ignored) {
            }
        }

        winScore = getInt(props, "win_score", winScore);
        cardFile = props.getProperty("card_file", cardFile).trim();
        gems2Players = getInt(props, "gems_2_players", gems2Players);
        gems3Players = getInt(props, "gems_3_players", gems3Players);
        gems4Players = getInt(props, "gems_4_players", gems4Players);
        nobles2Players = getInt(props, "nobles_2_players", nobles2Players);
        nobles3Players = getInt(props, "nobles_3_players", nobles3Players);
        nobles4Players = getInt(props, "nobles_4_players", nobles4Players);
        goldCoins = getInt(props, "gold_coins", goldCoins);
    }

    private int getInt(Properties props, String key, int defaultVal) {
        String val = props.getProperty(key);
        if (val == null)
            return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /** Returns the initial gem count for the given number of players (2-4). */
    public int getGemCount(int numPlayers) {
        return switch (numPlayers) {
            case 3 -> gems3Players;
            case 4 -> gems4Players;
            default -> gems2Players; // 2 players is the default
        };
    }

    /** Returns the number of nobles for the given number of players (2-4). */
    public int getNobleCount(int numPlayers) {
        return switch (numPlayers) {
            case 3 -> nobles3Players;
            case 4 -> nobles4Players;
            default -> nobles2Players;
        };
    }

    // Getters
    public int getWinScore() {
        return winScore;
    }

    public String getCardFile() {
        return cardFile;
    }

    public int getGoldCoins() {
        return goldCoins;
    }
}