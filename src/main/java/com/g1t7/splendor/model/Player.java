package com.g1t7.splendor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Player implements Serializable {

    // --- CONSTANTS ---
    public static final int MAX_COIN_LIMIT = 10;
    public static final int DISCONNECT_TIMEOUT_MS = 30000;
    public static final int TOTAL_COIN_TYPES = 6;
    public static final int REGULAR_GEM_TYPES = 5;

    private Game game;
    private boolean online = true;
    private boolean ai = false;
    private String name;
    private int score;
    private int[] myCoins = new int[TOTAL_COIN_TYPES];
    private int[] myCards = new int[REGULAR_GEM_TYPES];
    private List<Card> reservedCards = new ArrayList<>();
    private Stack<Card> cards = new Stack<>();
    private List<Noble> obtainedNobles = new ArrayList<>();

    // --- MULTIPLAYER FIELDS ---
    private String uuid;
    private boolean isReady = false;
    private boolean isEjected = false;
    private long lastHeartbeat = System.currentTimeMillis();

    public Player() {
    }

    public Player(Game game, String name) {
        this.game = game;
        this.name = name;
    }

    public Player(Game game, String name, boolean ai) {
        this.game = game;
        this.name = name;
        this.ai = ai;
    }

    public boolean isDisconnected() {
        if (ai || isEjected)
            return false;
        return (System.currentTimeMillis() - lastHeartbeat) > DISCONNECT_TIMEOUT_MS;
    }

    public int getTotalCoins() {
        int total = 0;
        for (int c : myCoins)
            total += c;
        return total;
    }

    // --- GETTERS AND SETTERS ---
    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

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

    public int[] getMyCoins() {
        return myCoins;
    }

    public void setMyCoins(int[] myCoins) {
        this.myCoins = myCoins;
    }

    public int[] getMyCards() {
        return myCards;
    }

    public void setMyCards(int[] myCards) {
        this.myCards = myCards;
    }

    public List<Card> getReservedCards() {
        return reservedCards;
    }

    public Stack<Card> getCards() {
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