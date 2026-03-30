package com.g1t7.splendor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Game implements Serializable {

    private GameConfig config;
    private int[] bankCoins = new int[6];
    private List<Card> tier1Deck = new ArrayList<>();
    private List<Card> tier2Deck = new ArrayList<>();
    private List<Card> tier3Deck = new ArrayList<>();
    private List<Card> visibleCards = new ArrayList<>(12);
    private List<Noble> activeNobles = new ArrayList<>();
    private List<Player> players = new ArrayList<>();

    private Card pendingCard = null;
    private int nowTurn = 0;
    private int startingPlayer = 0;
    private String message = "";
    private boolean finalRound = false;
    private boolean gameOver = false;
    private boolean pendingDiscard = false;

    // --- NEW MULTIPLAYER FIELDS ---
    private int capacity = 2;
    private boolean started = false;
    private String hostUuid;
    private long lastActivityTime = System.currentTimeMillis();

    public Game() {
        config = new GameConfig();
    }

    public void variableInit() {
        int numPlayers = players.size();
        if (numPlayers < 2)
            numPlayers = 2;

        int gemCount = config.getGemCount(numPlayers);
        int goldCount = config.getGoldCoins();
        bankCoins = new int[] { gemCount, gemCount, gemCount, gemCount, gemCount, goldCount };

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

        for (int i = 0; i < 12; i++)
            visibleCards.add(null);
        for (int slot = 0; slot < 4; slot++)
            dealSlot(slot);
        for (int slot = 4; slot < 8; slot++)
            dealSlot(slot);
        for (int slot = 8; slot < 12; slot++)
            dealSlot(slot);

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

    public void changeTurns() {
        this.lastActivityTime = System.currentTimeMillis(); // Reset idle timer
        pendingCard = null;
        message = "";

        if (!finalRound && getCurrentPlayer().getScore() >= config.getWinScore()) {
            finalRound = true;
        }

        // Loop to find the next player who isn't ejected
        int nextTurn = nowTurn;
        int attempts = 0;
        do {
            nextTurn = (nextTurn + 1) % players.size();
            attempts++;
        } while (players.get(nextTurn).isEjected() && attempts < players.size());

        // If everyone was ejected somehow, end game
        if (attempts >= players.size()) {
            gameOver = true;
            return;
        }

        if (finalRound && nextTurn == startingPlayer) {
            gameOver = true;
            return;
        }

        nowTurn = nextTurn;

        Player next = getCurrentPlayer();
        if (next.isAi() && !gameOver) {
            boolean acted = AIPlayer.takeTurn(this, next);
            if (acted)
                changeTurns();
        }
    }

    public Player getCurrentPlayer() {
        return players.get(nowTurn);
    }

    public void replenishCard(int slotIndex) {
        List<Card> deck = deckForSlot(slotIndex);
        if (!deck.isEmpty())
            visibleCards.set(slotIndex, deck.remove(deck.size() - 1));
        else
            visibleCards.set(slotIndex, null);
    }

    public boolean isGameOver() {
        return gameOver;
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

    // --- EXISTING GETTERS/SETTERS ---
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

    public boolean isPendingDiscard() {
        return pendingDiscard;
    }

    public void setPendingDiscard(boolean pendingDiscard) {
        this.pendingDiscard = pendingDiscard;
    }

    // --- NEW MULTIPLAYER GETTERS/SETTERS ---
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
}