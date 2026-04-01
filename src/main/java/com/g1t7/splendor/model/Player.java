package com.g1t7.splendor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player in the Splendor game.
 * This is a pure data model (POJO) containing the player's state.
 */
public class Player implements Serializable {

    // --- CONSTANTS ---
    public static final int MAX_COIN_LIMIT = 10;
    public static final int DISCONNECT_TIMEOUT_MS = 30000;
    public static final int TOTAL_COIN_TYPES = 6;
    public static final int REGULAR_GEM_TYPES = 5;

    private boolean ai = false;
    private String name;
    private int score;

    // Clean Code: Renamed from 'myCoins' and 'myCards'
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

    public Player() {
    }

    // FIXED: Removed the 'Game' object from the constructor to break the circular
    // dependency
    public Player(String name) {
        this.name = name;
    }

    public Player(String name, boolean ai) {
        this.name = name;
        this.ai = ai;
    }

    /**
     * Checks if a human player has timed out due to inactivity.
     * 
     * @return true if the player has disconnected, false otherwise.
     */
    public boolean isDisconnected() {
        if (ai || isEjected)
            return false;
        return (System.currentTimeMillis() - lastHeartbeat) > DISCONNECT_TIMEOUT_MS;
    }

    /**
     * Calculates the total number of coins currently held by the player.
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

    // FIXED: Renamed Getters/Setters
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