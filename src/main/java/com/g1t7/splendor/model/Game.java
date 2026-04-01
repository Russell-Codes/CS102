package com.g1t7.splendor.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.g1t7.splendor.config.GameConfig;

public class Game {

    // --- CONSTANTS ---
    public static final int VISIBLE_CARDS_PER_TIER = 4;
    public static final int TOTAL_VISIBLE_CARDS = 12;
    public static final int TOTAL_COIN_TYPES = 6;

    private GameConfig config;
    private int[] bankCoins = new int[TOTAL_COIN_TYPES];
    private List<Card> tier1Deck = new ArrayList<>();
    private List<Card> tier2Deck = new ArrayList<>();
    private List<Card> tier3Deck = new ArrayList<>();
    private List<Card> visibleCards = new ArrayList<>(TOTAL_VISIBLE_CARDS);
    private List<Noble> activeNobles = new ArrayList<>();
    private List<Player> players = new ArrayList<>();

    private int currentTurnIndex = 0;
    private String message = "";
    private boolean finalRound = false;
    private boolean gameOver = false;
    private boolean pendingDiscard = false;
    private boolean pendingNobleChoice = false;
    private List<Noble> pendingNobles = new ArrayList<>();

    // --- MULTIPLAYER FIELDS ---
    private int capacity = 2;
    private boolean started = false;
    private String hostUuid;
    private long lastActivityTime = System.currentTimeMillis();

    public Game() {
        config = new GameConfig();
    }

    public Player getCurrentPlayer() {
        return players.get(currentTurnIndex);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public Player getWinner() {
        return players.stream()
                .max(Comparator.comparingInt(Player::getScore)
                        .thenComparing(Comparator.comparingInt((Player p) -> p.getCards().size()).reversed()))
                .orElse(players.get(0));
    }

    public String gemColorName(int i) {
        return GemColor.fromIndex(i).name().toLowerCase();
    }

    public List<Card> getVisibleCardsForTier(int tier) {
        int start = (tier - 1) * VISIBLE_CARDS_PER_TIER;
        return visibleCards.subList(start, start + VISIBLE_CARDS_PER_TIER);
    }

    public int getDeckSize(int tier) {
        if (tier == 1)
            return tier1Deck.size();
        if (tier == 2)
            return tier2Deck.size();
        return tier3Deck.size();
    }

    public int getWinScore() {
        return config.getWinScore();
    }

    // --- GETTERS/SETTERS ---
    public GameConfig getConfig() {
        return config;
    }

    public int[] getBankCoins() {
        return bankCoins;
    }

    public void setBankCoins(int[] bankCoins) {
        this.bankCoins = bankCoins;
    }

    public List<Card> getTier1Deck() {
        return tier1Deck;
    }

    public List<Card> getTier2Deck() {
        return tier2Deck;
    }

    public List<Card> getTier3Deck() {
        return tier3Deck;
    }

    public List<Card> getVisibleCards() {
        return visibleCards;
    }

    public List<Noble> getActiveNobles() {
        return activeNobles;
    }

    public void setActiveNobles(List<Noble> activeNobles) {
        this.activeNobles = activeNobles;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }


    public int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    public void setCurrentTurnIndex(int currentTurnIndex) {
        this.currentTurnIndex = currentTurnIndex;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isFinalRound() {
        return finalRound;
    }

    public void setFinalRound(boolean finalRound) {
        this.finalRound = finalRound;
    }

    public boolean isPendingDiscard() {
        return pendingDiscard;
    }

    public void setPendingDiscard(boolean pendingDiscard) {
        this.pendingDiscard = pendingDiscard;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public boolean isPendingNobleChoice() {
        return pendingNobleChoice;
    }

    public void setPendingNobleChoice(boolean pendingNobleChoice) {
        this.pendingNobleChoice = pendingNobleChoice;
    }

    public List<Noble> getPendingNobles() {
        return pendingNobles;
    }

    public void setPendingNobles(List<Noble> pendingNobles) {
        this.pendingNobles = pendingNobles;
    }
}