package com.g1t7.splendor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * Loads game settings and data file paths.
 *
 * <p>It tries an external config file first, then the classpath copy,
 * and finally falls back to defaults in this class.
 */
public class GameConfig {

    private static final Logger logger = LoggerFactory.getLogger(GameConfig.class);
    private final String CONFIG_PATH = "game/config/config.properties";

    private int winScore = 15;
    private String cardFile = "game/config/cards.csv";
    private String nobleFile = "game/config/nobles.csv";

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

    /**
        * Creates a config object and loads values immediately.
     */
    public GameConfig() {
        load();
    }

    /**
        * Loads values from file/classpath and keeps defaults for anything missing or invalid.
     */
    private void load() {
        Properties props = new Properties();
        File file = new File(CONFIG_PATH);

        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                props.load(is);
            } catch (IOException e) {
                logger.error("Error loading config from file: {}", CONFIG_PATH, e);
            }
        } else {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(this.CONFIG_PATH)) {
                if (is != null)
                    props.load(is);
            } catch (IOException e) {
                logger.error("Error loading config from classpath: {}", this.CONFIG_PATH, e);
            }
        }

        winScore = getInt(props, "win_score", winScore);
        cardFile = props.getProperty("card_file", cardFile).trim();
        nobleFile = props.getProperty("noble_file", nobleFile).trim();
        gems2Players = getInt(props, "gems_2_players", gems2Players);
        gems3Players = getInt(props, "gems_3_players", gems3Players);
        gems4Players = getInt(props, "gems_4_players", gems4Players);
        nobles2Players = getInt(props, "nobles_2_players", nobles2Players);
        nobles3Players = getInt(props, "nobles_3_players", nobles3Players);
        nobles4Players = getInt(props, "nobles_4_players", nobles4Players);

        logger.info(
                "Loaded Configuration: winScore={}, cards={}, nobles={}, gems(2/3/4)={}/{}/{}, nobles(2/3/4)={}/{}/{}",
                winScore, cardFile, nobleFile, gems2Players, gems3Players, gems4Players, nobles2Players, nobles3Players,
                nobles4Players);
    }

    /**
        * Reads an integer property, with fallback on missing/invalid values.
     *
     * @param props source properties map
     * @param key configuration key to resolve
     * @param defaultVal fallback value when key is missing or invalid
    * @return parsed value or default
     */
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

    /**
        * Gets per-color gem count for the given player count.
     *
     * @param numPlayers current number of players
    * @return gems per color in the bank
     */
    public int getGemCount(int numPlayers) {
        return switch (numPlayers) {
            case 3 -> gems3Players;
            case 4 -> gems4Players;
            default -> gems2Players;
        };
    }

    /**
        * Gets how many nobles should be in play for the player count.
     *
     * @param numPlayers current number of players
     * @return number of nobles in play
     */
    public int getNobleCount(int numPlayers) {
        return switch (numPlayers) {
            case 3 -> nobles3Players;
            case 4 -> nobles4Players;
            default -> nobles2Players;
        };
    }

    public int getWinScore() {
        return winScore;
    }

    public String getCardFile() {
        return cardFile;
    }

    public String getNobleFile() {
        return nobleFile;
    }

    public int getGoldCoins() {
        return goldCoins;
    }
}