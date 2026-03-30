package com.g1t7.splendor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class Player implements Serializable {

    private Game game;
    private boolean online = true;
    private boolean ai = false;
    private String name;
    private int score;
    private int[] mycoins = new int[6];
    private int[] mycards = new int[5];
    private List<Card> reservedCards = new ArrayList<>();
    private Stack<Card> cards = new Stack<>();
    private List<Noble> obtainedNobles = new ArrayList<>();

    // --- NEW MULTIPLAYER FIELDS ---
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

    // --- HELPER FOR DISCONNECTS ---
    public boolean isDisconnected() {
        // If it's an AI or they've been ejected, they technically aren't disconnected
        if (ai || isEjected)
            return false;
        // Considered disconnected if no ping for 30 seconds
        return (System.currentTimeMillis() - lastHeartbeat) > 30000;
    }

    // --- EXISTING GAME LOGIC ---
    public boolean buyCard(Card card) {
        int[] bankCoins = game.getBankCoins();
        int[] effectiveCost = new int[5];
        for (int i = 0; i < 5; i++) {
            effectiveCost[i] = Math.max(0, card.getCost()[i] - mycards[i]);
        }

        int goldNeeded = 0;
        for (int i = 0; i < 5; i++) {
            int shortfall = Math.max(0, effectiveCost[i] - mycoins[i]);
            goldNeeded += shortfall;
        }
        if (goldNeeded > mycoins[5]) {
            game.setMessage("Not enough coins to buy this card.");
            return false;
        }

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

        cards.push(card);
        mycards[card.getGemColor().ordinal()]++;
        score += card.getValue();
        card.setReserved(false);

        checkNobles(game.getActiveNobles());
        return true;
    }

    public boolean exchangeCoin(List<String> selectedColors) {
        if (selectedColors == null || selectedColors.isEmpty() || selectedColors.size() > 3) {
            game.setMessage("Select 1-3 coins.");
            return false;
        }

        int[] bankCoins = game.getBankCoins();
        int[] toTake = new int[6];

        for (String colorName : selectedColors) {
            GemColor gc;
            try {
                gc = GemColor.valueOf(colorName.toUpperCase());
            } catch (IllegalArgumentException e) {
                game.setMessage("Invalid gem color.");
                return false;
            }
            if (gc == GemColor.GOLD) {
                game.setMessage("Cannot take gold coins directly.");
                return false;
            }
            toTake[gc.ordinal()]++;
        }

        if (selectedColors.size() == 2) {
            int distinctCount = 0;
            int sameColorIdx = -1;
            for (int i = 0; i < 5; i++) {
                if (toTake[i] > 0) {
                    distinctCount++;
                    sameColorIdx = i;
                }
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

        for (int i = 0; i < 5; i++) {
            mycoins[i] += toTake[i];
            bankCoins[i] -= toTake[i];
        }
        return true;
    }

    public boolean escortCard(Card card) {
        if (reservedCards.size() >= 3) {
            game.setMessage("You can only reserve up to 3 cards.");
            return false;
        }
        card.setReserved(true);
        reservedCards.add(card);
        if (game.getBankCoins()[GemColor.GOLD.ordinal()] > 0) {
            mycoins[GemColor.GOLD.ordinal()]++;
            game.getBankCoins()[GemColor.GOLD.ordinal()]--;
        }
        return true;
    }

    public void checkNobles(List<Noble> nobles) {
        if (nobles == null)
            return;

        List<Noble> satisfied = new ArrayList<>();
        for (Noble noble : nobles) {
            if (noble.isSatisfiedBy(mycards)) {
                satisfied.add(noble);
            }
        }

        // If exactly one noble is satisfied, apply it instantly
        if (satisfied.size() == 1) {
            score += satisfied.get(0).getVictoryPoints();
            this.obtainedNobles.add(satisfied.get(0));
            nobles.remove(satisfied.get(0));
        } else if (satisfied.size() > 1) {
            if (this.ai) {
                score += satisfied.get(0).getVictoryPoints();
                this.obtainedNobles.add(satisfied.get(0));
                nobles.remove(satisfied.get(0));
            } else {
                game.setPendingNobleChoice(true);
                game.setPendingNobles(satisfied);
            }
        }
    }

    public int getTotalCoins() {
        int total = 0;
        for (int c : mycoins)
            total += c;
        return total;
    }

    public boolean discardCoin(String color) {
        GemColor gc;
        try {
            gc = GemColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
        int idx = gc.ordinal();
        if (mycoins[idx] <= 0)
            return false;
        mycoins[idx]--;
        game.getBankCoins()[idx]++;
        return true;
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

    public int[] getMycoins() {
        return mycoins;
    }

    public void setMycoins(int[] mycoins) {
        this.mycoins = mycoins;
    }

    public int[] getMycards() {
        return mycards;
    }

    public void setMycards(int[] mycards) {
        this.mycards = mycards;
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
        isReady = ready;
    }

    public boolean isEjected() {
        return isEjected;
    }

    public void setEjected(boolean ejected) {
        isEjected = ejected;
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