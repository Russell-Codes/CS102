package com.g1t7.splendor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

public class Player implements Serializable {


    private Game game;
    private boolean online = true;
    private boolean ai = false;
    private String name;
    private int score;
    // mycoins[i] = coins held, indexed by GemColor ordinal (0=WHITE..5=GOLD)
    private int[] mycoins = new int[6];
    // mycards[i] = card bonus (purchased cards) per GemColor ordinal (0=WHITE..4=BLACK)
    private int[] mycards = new int[5];
    private List<Card> reservedCards = new ArrayList<>();
    private Stack<Card> cards = new Stack<>();

    public Player() {}

    public Player(Game game, String name) {
        this.game = game;
        this.name = name;
    }

    public Player(Game game, String name, boolean ai) {
        this.game = game;
        this.name = name;
        this.ai = ai;
    }

    // -------------------------------------------------------------------------
    // Game actions
    // -------------------------------------------------------------------------

    /**
     * Attempt to buy a card. Auto-deducts coins: colored first, gold covers shortfall.
     * return true if purchase succeeded
     */
    public boolean buyCard(Card card) {
        int[] bankCoins = game.getBankCoins();
        // Compute effective cost after card bonuses reduce it
        int[] effectiveCost = new int[5];
        for (int i = 0; i < 5; i++) {
            effectiveCost[i] = Math.max(0, card.getCost()[i] - mycards[i]);
        }

        // Check if player can afford (colored coins + gold for shortfall)
        int goldNeeded = 0;
        for (int i = 0; i < 5; i++) {
            int shortfall = Math.max(0, effectiveCost[i] - mycoins[i]);
            goldNeeded += shortfall;
        }
        if (goldNeeded > mycoins[5]) {
            game.setMessage("Not enough coins to buy this card.");
            return false;
        }

        // Deduct coins
        int goldSpent = 0;
        for (int i = 0; i < 5; i++) {
            int paid = Math.min(mycoins[i], effectiveCost[i]);
            mycoins[i] -= paid;
            bankCoins[i] += paid;
            int shortfall = effectiveCost[i] - paid;
            goldSpent += shortfall;
        }
        mycoins[5] -= goldSpent;
        bankCoins[5] += goldSpent;

        // Record purchase
        cards.push(card);
        mycards[card.getGemColor().ordinal()]++;
        score += card.getValue();
        card.setReserved(false);

        // Check for noble visits
        checkNobles(game.getActiveNobles());

        return true;
    }

    /**
     * Take coins from the bank according to Splendor rules.
     * selectedColors: list of GemColor strings for coins to take.
     * Rule A: 2 coins of same color → bank must have ≥ 4 of that color.
     * Rule B: up to 3 different colors → bank has ≥ 1 each.
     * Player coin total must not exceed 10.
     */
    public boolean exchangeCoin(List<String> selectedColors) {
        if (selectedColors == null || selectedColors.isEmpty() || selectedColors.size() > 3) {
            game.setMessage("Select 1–3 coins.");
            return false;
        }

        int[] bankCoins = game.getBankCoins();
        int[] toTake = new int[6];

        // Tally requested coins
        for (String colorName : selectedColors) {
            GemColor gc;
            try {
                gc = GemColor.valueOf(colorName.toUpperCase());
            } catch (IllegalArgumentException e) {
                game.setMessage("Invalid gem color: " + colorName);
                return false;
            }
            if (gc == GemColor.GOLD) {
                game.setMessage("Cannot take gold coins directly.");
                return false;
            }
            toTake[gc.ordinal()]++;
        }

        // Validate rule
        if (selectedColors.size() == 2) {
            // Must be same color, bank ≥ 4
            int distinctCount = 0;
            int sameColorIdx = -1;
            for (int i = 0; i < 5; i++) {
                if (toTake[i] > 0) { distinctCount++; sameColorIdx = i; }
            }
            if (distinctCount != 1 || toTake[sameColorIdx] != 2) {
                game.setMessage("When taking 2 coins, they must be the same color.");
                return false;
            }
            if (bankCoins[sameColorIdx] < 4) {
                game.setMessage("Bank needs at least 4 of that color to take 2.");
                return false;
            }
        } else {
            // 1 or 3 coins: all different colors, bank ≥ 1 each
            for (int i = 0; i < 5; i++) {
                if (toTake[i] > 1) {
                    game.setMessage("Select different colors (or same color for 2-coin take).");
                    return false;
                }
                if (toTake[i] == 1 && bankCoins[i] < 1) {
                    game.setMessage("Bank is empty for that gem color.");
                    return false;
                }
            }
        }

        // Validate hand limit
        if (getTotalCoins() + selectedColors.size() > 10) {
            game.setMessage("You cannot hold more than 10 coins.");
            return false;
        }

        // Apply
        for (int i = 0; i < 5; i++) {
            mycoins[i] += toTake[i];
            bankCoins[i] -= toTake[i];
        }
        return true;
    }

    /**
     * Reserve a card (from the visible board or deck).
     * @return true if reservation succeeded
     */
    public boolean escortCard(Card card) {
        if (reservedCards.size() >= 3) {
            game.setMessage("You can only reserve up to 3 cards.");
            return false;
        }
        card.setReserved(true);
        reservedCards.add(card);
        // Award gold coin if available
        if (game.getBankCoins()[GemColor.GOLD.ordinal()] > 0) {
            mycoins[GemColor.GOLD.ordinal()]++;
            game.getBankCoins()[GemColor.GOLD.ordinal()]--;
        }
        return true;
    }

    /**
     * Check if any active noble's requirements are satisfied; award first match.
     */
    public void checkNobles(List<Noble> nobles) {
        if (nobles == null) return;
        Iterator<Noble> it = nobles.iterator();
        while (it.hasNext()) {
            Noble noble = it.next();
            if (noble.isSatisfiedBy(mycards)) {
                score += noble.getVictoryPoints();
                it.remove();
                break; // one noble per turn
            }
        }
    }

    public int getTotalCoins() {
        int total = 0;
        for (int c : mycoins) total += c;
        return total;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public boolean isAi() { return ai; }
    public void setAi(boolean ai) { this.ai = ai; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int[] getMycoins() { return mycoins; }
    public void setMycoins(int[] mycoins) { this.mycoins = mycoins; }
    public int[] getMycards() { return mycards; }
    public void setMycards(int[] mycards) { this.mycards = mycards; }
    public List<Card> getReservedCards() { return reservedCards; }
    public Stack<Card> getCards() { return cards; }
}