package com.g1t7.splendor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Game implements Serializable {

    private GameConfig config;

    // bankCoins[i] = number of coins in bank, indexed by GemColor ordinal
    private int[] bankCoins = new int[6];

    private List<Card> tier1Deck = new ArrayList<>();
    private List<Card> tier2Deck = new ArrayList<>();
    private List<Card> tier3Deck = new ArrayList<>();
    // visibleCards: indices 0-3 = tier1, 4-7 = tier2, 8-11 = tier3; null = empty
    // slot
    private List<Card> visibleCards = new ArrayList<>(12);

    private List<Noble> activeNobles = new ArrayList<>();
    private List<Player> players = new ArrayList<>();

    private Card pendingCard = null;
    private int nowTurn = 0;
    private int startingPlayer = 0; // who started this round
    private String message = "";
    private boolean finalRound = false; // set true once any player hits win score
    private boolean gameOver = false;

    public Game() {
        config = new GameConfig();
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    public void variableInit() {
        int numPlayers = players.size();
        if (numPlayers < 2)
            numPlayers = 2;

        int gemCount = config.getGemCount(numPlayers);
        int goldCount = config.getGoldCoins();
        bankCoins = new int[] { gemCount, gemCount, gemCount, gemCount, gemCount, goldCount };

        // Build and shuffle decks from CSV
        List<Card> allCards = CardData.buildDeck(config.getCardFile());
        for (Card c : allCards) {
            if (c.getTier() == 1)
                tier1Deck.add(c);
            else if (c.getTier() == 2)
                tier2Deck.add(c);
            else
                tier3Deck.add(c);
        }
        Collections.shuffle(tier1Deck);
        Collections.shuffle(tier2Deck);
        Collections.shuffle(tier3Deck);

        // Deal 4 face-up per tier (12 slots total)
        for (int i = 0; i < 12; i++)
            visibleCards.add(null);
        for (int slot = 0; slot < 4; slot++)
            dealSlot(slot);
        for (int slot = 4; slot < 8; slot++)
            dealSlot(slot);
        for (int slot = 8; slot < 12; slot++)
            dealSlot(slot);

        // Nobles: shuffle, keep (numPlayers + 1) per standard rules
        int nobleCount = config.getNobleCount(numPlayers);
        List<Noble> allNobles = NobleData.buildNobles();
        Collections.shuffle(allNobles);
        activeNobles = new ArrayList<>(allNobles.subList(0, Math.min(nobleCount, allNobles.size())));
    }

    private void dealSlot(int slot) {
        List<Card> deck = deckForSlot(slot);
        if (!deck.isEmpty()) {
            visibleCards.set(slot, deck.remove(deck.size() - 1));
        }
    }

    private List<Card> deckForSlot(int slot) {
        if (slot < 4)
            return tier1Deck;
        if (slot < 8)
            return tier2Deck;
        return tier3Deck;
    }

    // -------------------------------------------------------------------------
    // Turn management
    // -------------------------------------------------------------------------

    /**
     * Advance to the next player's turn.
     * Implements the Splendor end-of-game rule: once any player reaches the
     * win score, the current round finishes (so every player gets equal turns),
     * *then* the game ends.
     */
    public void changeTurns() {
        pendingCard = null;
        message = "";

        // Check if current player triggered the final round
        if (!finalRound && getCurrentPlayer().getScore() >= config.getWinScore()) {
            finalRound = true;
        }

        int nextTurn = (nowTurn + 1) % players.size();

        // If we've completed a full round back to the starting player, and the
        // final round was triggered, the game is over.
        if (finalRound && nextTurn == startingPlayer) {
            gameOver = true;
            return;
        }

        nowTurn = nextTurn;

        // If the next player is AI, execute their turn automatically
        Player next = getCurrentPlayer();
        if (next.isAi() && !gameOver) {
            boolean acted = AIPlayer.takeTurn(this, next);
            if (acted) {
                changeTurns(); // recursively advance past all consecutive AI players
            }
        }
    }

    public Player getCurrentPlayer() {
        return players.get(nowTurn);
    }

    // -------------------------------------------------------------------------
    // Card replenishment
    // -------------------------------------------------------------------------

    public void replenishCard(int slotIndex) {
        List<Card> deck = deckForSlot(slotIndex);
        if (!deck.isEmpty()) {
            visibleCards.set(slotIndex, deck.remove(deck.size() - 1));
        } else {
            visibleCards.set(slotIndex, null);
        }
    }

    // -------------------------------------------------------------------------
    // Game-over helpers
    // -------------------------------------------------------------------------

    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Determine the winner.
     * Highest score wins; tie-breaker: fewest purchased development cards.
     */
    public Player getWinner() {
        return players.stream()
                .max(Comparator
                        .comparingInt(Player::getScore)
                        .thenComparing(Comparator.comparingInt((Player p) -> p.getCards().size()).reversed()))
                .orElse(players.get(0));
    }

    // -------------------------------------------------------------------------
    // Thymeleaf helpers
    // -------------------------------------------------------------------------

    public String gemColorName(int i) {
        return GemColor.fromIndex(i).name().toLowerCase();
    }

    public List<Card> getVisibleCardsForTier(int tier) {
        int start = (tier - 1) * 4;
        return visibleCards.subList(start, start + 4);
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

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

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

    public Card getPendingCard() {
        return pendingCard;
    }

    public void setPendingCard(Card pendingCard) {
        this.pendingCard = pendingCard;
    }

    public int getNowTurn() {
        return nowTurn;
    }

    public void setNowTurn(int nowTurn) {
        this.nowTurn = nowTurn;
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
}