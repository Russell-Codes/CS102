package com.g1t7.splendor.model;

import java.io.Serializable;
import java.util.*;

/**
 * Simple computer opponent for Splendor.
 * Strategy priority:
 * 1. Buy the highest-VP affordable card (board or reserved).
 * 2. Reserve a high-VP card that is close to affordable.
 * 3. Take coins toward the cheapest affordable card's remaining cost.
 */
public class AIPlayer implements Serializable {

    private AIPlayer() {
    }

    /**
     * Execute one AI turn for the given player in the given game.
     * Returns true if a valid action was taken.
     */
    public static boolean takeTurn(Game game, Player player) {
        // 1. Try to buy the best affordable card (highest VP first)
        if (tryBuyBestCard(game, player))
            return true;

        // 2. Try to reserve a high-VP card if we have room
        if (tryReserveCard(game, player))
            return true;

        // 3. Take coins toward the cheapest needed card cost
        return tryTakeCoins(game, player);
    }

    // -------------------------------------------------------------------------
    // Strategy 1: Buy the best card we can afford
    // -------------------------------------------------------------------------
    private static boolean tryBuyBestCard(Game game, Player player) {
        Card best = null;
        int bestSlot = -1;
        boolean fromReserve = false;

        // Check visible board cards
        List<Card> visible = game.getVisibleCards();
        for (int i = 0; i < visible.size(); i++) {
            Card c = visible.get(i);
            if (c != null && canAfford(player, c)) {
                if (best == null || c.getValue() > best.getValue()) {
                    best = c;
                    bestSlot = i;
                    fromReserve = false;
                }
            }
        }

        // Check reserved cards
        for (int i = 0; i < player.getReservedCards().size(); i++) {
            Card c = player.getReservedCards().get(i);
            if (canAfford(player, c)) {
                if (best == null || c.getValue() > best.getValue()) {
                    best = c;
                    bestSlot = i;
                    fromReserve = true;
                }
            }
        }

        if (best == null)
            return false;

        boolean ok = player.buyCard(best);
        if (ok) {
            if (fromReserve) {
                player.getReservedCards().remove(best);
            } else {
                game.replenishCard(bestSlot);
            }
        }
        return ok;
    }

    private static boolean canAfford(Player player, Card card) {
        int goldNeeded = 0;
        for (int i = 0; i < 5; i++) {
            int effective = Math.max(0, card.getCost()[i] - player.getMycards()[i]);
            int shortfall = Math.max(0, effective - player.getMycoins()[i]);
            goldNeeded += shortfall;
        }
        return goldNeeded <= player.getMycoins()[5];
    }

    // -------------------------------------------------------------------------
    // Strategy 2: Reserve a high-VP card
    // -------------------------------------------------------------------------
    private static boolean tryReserveCard(Game game, Player player) {
        if (player.getReservedCards().size() >= 3)
            return false;

        Card best = null;
        int bestSlot = -1;

        List<Card> visible = game.getVisibleCards();
        for (int i = 0; i < visible.size(); i++) {
            Card c = visible.get(i);
            if (c != null && c.getValue() >= 2) { // Only reserve valuable cards
                if (best == null || c.getValue() > best.getValue()) {
                    best = c;
                    bestSlot = i;
                }
            }
        }

        if (best == null)
            return false;

        boolean ok = player.escortCard(best);
        if (ok) {
            game.replenishCard(bestSlot);
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // Strategy 3: Take coins
    // -------------------------------------------------------------------------
    private static boolean tryTakeCoins(Game game, Player player) {
        if (player.getTotalCoins() >= 10) {
            // Can't take more coins; pass turn
            return true; // treat as valid action (pass)
        }

        int[] bankCoins = game.getBankCoins();

        // Find the cheapest visible card's remaining cost to guide coin selection
        int[] need = computeNeeds(game, player);

        // Prefer taking 3 different colours we need
        List<String> selection = new ArrayList<>();
        int remaining = Math.min(3, 10 - player.getTotalCoins()); // respect hand limit

        // First pass: take colours we need
        for (int i = 0; i < 5 && selection.size() < remaining; i++) {
            if (need[i] > 0 && bankCoins[i] >= 1) {
                selection.add(GemColor.fromIndex(i).name());
            }
        }

        // Second pass: fill with any available colour
        for (int i = 0; i < 5 && selection.size() < remaining; i++) {
            String name = GemColor.fromIndex(i).name();
            if (!selection.contains(name) && bankCoins[i] >= 1) {
                selection.add(name);
            }
        }

        // If we can only take 0 or 1, try taking 2 of the same if bank ≥ 4
        if (selection.size() <= 1) {
            for (int i = 0; i < 5; i++) {
                if (bankCoins[i] >= 4 && player.getTotalCoins() + 2 <= 10) {
                    selection.clear();
                    String name = GemColor.fromIndex(i).name();
                    selection.add(name);
                    selection.add(name);
                    break;
                }
            }
        }

        if (selection.isEmpty())
            return true; // no coins available; pass

        return player.exchangeCoin(selection);
    }

    /**
     * Compute total coin need (after bonuses) across the cheapest visible cards.
     */
    private static int[] computeNeeds(Game game, Player player) {
        int[] need = new int[5];
        Card target = null;
        int minTotalCost = Integer.MAX_VALUE;

        for (Card c : game.getVisibleCards()) {
            if (c == null)
                continue;
            int total = 0;
            for (int i = 0; i < 5; i++) {
                total += Math.max(0, c.getCost()[i] - player.getMycards()[i] - player.getMycoins()[i]);
            }
            if (total < minTotalCost) {
                minTotalCost = total;
                target = c;
            }
        }

        if (target != null) {
            for (int i = 0; i < 5; i++) {
                need[i] = Math.max(0, target.getCost()[i] - player.getMycards()[i] - player.getMycoins()[i]);
            }
        }
        return need;
    }
}