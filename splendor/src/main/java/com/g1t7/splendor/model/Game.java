package com.g1t7.splendor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game implements Serializable {


    // bankCoins[i] = number of coins in bank, indexed by GemColor ordinal
    private int[] bankCoins = new int[6];

    private List<Card> tier1Deck = new ArrayList<>();
    private List<Card> tier2Deck = new ArrayList<>();
    private List<Card> tier3Deck = new ArrayList<>();
    // visibleCards: indices 0-3 = tier1, 4-7 = tier2, 8-11 = tier3; null = empty slot
    private List<Card> visibleCards = new ArrayList<>(12);

    private List<Noble> activeNobles = new ArrayList<>();
    private Player[] players = new Player[2];

    private Card pendingCard = null;
    private int nowTurn = 0;
    private String message = "";

    public Game() {}

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    public void variableInit() {
        // Standard 2-player bank: 4 of each gem, 5 gold
        bankCoins = new int[]{4, 4, 4, 4, 4, 5};

        // Build and shuffle decks
        List<Card> allCards = CardData.buildDeck();
        for (Card c : allCards) {
            if (c.getTier() == 1) tier1Deck.add(c);
            else if (c.getTier() == 2) tier2Deck.add(c);
            else tier3Deck.add(c);
        }
        Collections.shuffle(tier1Deck);
        Collections.shuffle(tier2Deck);
        Collections.shuffle(tier3Deck);

        // Deal 4 face-up per tier (12 slots total)
        for (int i = 0; i < 12; i++) visibleCards.add(null);
        for (int slot = 0; slot < 4; slot++) dealSlot(slot);
        for (int slot = 4; slot < 8; slot++) dealSlot(slot);
        for (int slot = 8; slot < 12; slot++) dealSlot(slot);

        // Nobles: shuffle, keep 3
        List<Noble> allNobles = NobleData.buildNobles();
        Collections.shuffle(allNobles);
        activeNobles = new ArrayList<>(allNobles.subList(0, Math.min(3, allNobles.size())));
    }

    private void dealSlot(int slot) {
        List<Card> deck = deckForSlot(slot);
        if (!deck.isEmpty()) {
            visibleCards.set(slot, deck.remove(deck.size() - 1));
        }
    }

    private List<Card> deckForSlot(int slot) {
        if (slot < 4) return tier1Deck;
        if (slot < 8) return tier2Deck;
        return tier3Deck;
    }

    // -------------------------------------------------------------------------
    // Turn management
    // -------------------------------------------------------------------------

    public void changeTurns() {
        pendingCard = null;
        message = "";
        nowTurn = (nowTurn + 1) % 2;
    }

    public Player getCurrentPlayer() {
        return players[nowTurn];
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
        return players[0].getScore() >= 15 || players[1].getScore() >= 15;
    }

    public Player getWinner() {
        if (players[0].getScore() > players[1].getScore()) return players[0];
        if (players[1].getScore() > players[0].getScore()) return players[1];
        // Tie: fewer cards wins (more cards = less efficient)
        return players[0].getCards().size() <= players[1].getCards().size()
                ? players[0] : players[1];
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
        if (tier == 1) return tier1Deck.size();
        if (tier == 2) return tier2Deck.size();
        return tier3Deck.size();
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int[] getBankCoins() { return bankCoins; }
    public void setBankCoins(int[] bankCoins) { this.bankCoins = bankCoins; }

    public List<Card> getTier1Deck() { return tier1Deck; }
    public List<Card> getTier2Deck() { return tier2Deck; }
    public List<Card> getTier3Deck() { return tier3Deck; }

    public List<Card> getVisibleCards() { return visibleCards; }

    public List<Noble> getActiveNobles() { return activeNobles; }
    public void setActiveNobles(List<Noble> activeNobles) { this.activeNobles = activeNobles; }

    public Player[] getPlayers() { return players; }
    public void setPlayers(Player[] players) { this.players = players; }

    public Card getPendingCard() { return pendingCard; }
    public void setPendingCard(Card pendingCard) { this.pendingCard = pendingCard; }

    public int getNowTurn() { return nowTurn; }
    public void setNowTurn(int nowTurn) { this.nowTurn = nowTurn; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
