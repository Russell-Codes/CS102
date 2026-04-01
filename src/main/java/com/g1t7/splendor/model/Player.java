package com.g1t7.splendor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores one player's game state.
 */
public class Player {

    // --- CONSTANTS ---
    public static final int MAX_COIN_LIMIT = 10;
    public static final int DISCONNECT_TIMEOUT_MS = 30000;
    public static final int TOTAL_COIN_TYPES = GemColor.values().length; // Includes GOLD
    public static final int REGULAR_GEM_TYPES = GemColor.values().length - 1; // Exclude GOLD

    private boolean ai = false;
    private String name;
    private int score;

    // Coin counts and permanent card bonuses.
    private int[] coins = new int[TOTAL_COIN_TYPES];
    private int[] bonuses = new int[REGULAR_GEM_TYPES];

    private List<Card> reservedCards = new ArrayList<>();
    private List<Card> cards = new ArrayList<>();
    private List<Noble> obtainedNobles = new ArrayList<>();

    // --- MULTIPLAYER FIELDS ---
    private String uuid;
    private boolean isReady = false;
    private boolean isEjected = false;
    private long lastHeartbeat = System.currentTimeMillis();

    /**
     * Creates a human player with the given display name.
     */
    public Player(String name) {
        this.name = name;
    }

    /**
     * Creates a player and sets whether it is AI-controlled.
     */
    public Player(String name, boolean ai) {
        this.name = name;
        this.ai = ai;
    }

    /**
     * Checks whether a human player timed out due to inactivity.
     * 
     * @return true if the player has disconnected, false otherwise.
     */
    public boolean isDisconnected() {
        if (ai || isEjected)
            return false;
        return (System.currentTimeMillis() - lastHeartbeat) > DISCONNECT_TIMEOUT_MS;
    }

    /**
     * Counts all coins currently held by the player.
     * 
     * @return the total coin count.
     */
    public int getTotalCoins() {
        int total = 0;
        for (int c : coins)
            total += c;
        return total;
    }

    // --- GETTERS AND SETTERS ---
    public boolean isAi() {
        return ai;
    }

    public void setAi(boolean ai) {
        this.ai = ai;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int[] getCoins() {
        return coins;
    }

    public void setCoins(int[] coins) {
        this.coins = coins;
    }

    public int[] getBonuses() {
        return bonuses;
    }

    public void setBonuses(int[] bonuses) {
        this.bonuses = bonuses;
    }

    public List<Card> getReservedCards() {
        return reservedCards;
    }

    public List<Card> getCards() {
        return cards;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    public boolean isEjected() {
        return isEjected;
    }

    public void setEjected(boolean ejected) {
        this.isEjected = ejected;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public List<Noble> getObtainedNobles() {
        return obtainedNobles;
    }
}