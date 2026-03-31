package com.g1t7.splendor.model;

import java.io.Serializable;
import java.util.*;

/**
 * Simple computer opponent for Splendor.
 * Implements a rigid, procedural, heuristic-driven decision matrix avoiding complex state-space trees.
 * Strategy priority waterfall:
 * 1. Buy the highest-VP affordable card (scans board then private reserves).
 * 2. Reserve a high-VP card (>= 2 VP) anticipating future affordability, disrupting opponents.
 * 3. Harvest bank coins aggressively targeting the cheapest visible board card.
 */
public class AIPlayer implements Serializable {

    private AIPlayer() {
    }

    /**
     * The primary deterministic entry point invoked automatically by the Game loop.
     * Evaluates the active board state strictly against a hard-coded 3-tier Boolean waterfall.
     *
     * @param game   The contextual, volatile parent game environment.
     * @param player The active polymorphic Player instance resolving logic.
     * @return boolean True if the state was mutated successfully via an action; false otherwise.
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

    /**
     * Top-tier execution policy: Profit Maximization.
     * Iterates O(N) over public board assets, tracking the absolute maximum victory point (VP) 
     * threshold matching affordability criteria. Re-evaluates O(M) over private withheld reserves,
     * overwriting max markers if strictly superior. Exits resolving purchase mutation if `best` is not null.
     */
    private static boolean tryBuyBestCard(Game game, Player player) {
        Card best = null;
        int bestSlot = -1;
        boolean fromReserve = false;

        // O(N) Iteration: Scan the public board's visible cards to isolate the maximum VP asset
        // that satisfies the player's current liquidity constraints.
        List<Card> visible = game.getVisibleCards();
        for (int i = 0; i < visible.size(); i++) {
            Card c = visible.get(i);
            // Ignore empty slots (null) and evaluate affordability
            if (c != null && canAfford(player, c)) {
                // Heuristic: Overwrite previous marker only if the new card yields strictly greater VP
                if (best == null || c.getValue() > best.getValue()) {
                    best = c;
                    bestSlot = i;
                    fromReserve = false;
                }
            }
        }

        // O(M) Iteration: Compare current best marker against the player's private reserved deck.
        // Private cards hold identical precedence to public ones but are prioritized dynamically based on VP yield.
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

    /**
     * Financial validation subroutine evaluating if current liquidity (coins + noble discounts) 
     * is >= exact requirement footprint of a targeted asset. Validations leverage the 5th ordinal
     * (Gold tokens) universally as wildcards.
     * 
     * @param player The resolving player.
     * @param card The targeted theoretical transaction asset.
     * @return boolean True if solvent.
     */
    private static boolean canAfford(Player player, Card card) {
        int goldNeeded = 0;
        // O(1) Iteration: Compare resource arrays. Constant boundary (0-4) corresponds to the
        // ordinal mappings of the core 5 colored gems (WHITE, BLUE, GREEN, RED, BLACK).
        for (int i = 0; i < 5; i++) {
            // Apply infinite noble discount deductions first, bounded rigidly at 0
            int effective = Math.max(0, card.getCost()[i] - player.getMyCards()[i]);
            // Assess required physical token expenditure, accumulating deficits as gold reliance
            int shortfall = Math.max(0, effective - player.getMyCoins()[i]);
            goldNeeded += shortfall;
        }
        // Final evaluation: Can wildcard Gold reserves (index 5) bridge the accumulated token deficit?
        return goldNeeded <= player.getMyCoins()[5];
    }

    // -------------------------------------------------------------------------
    // Strategy 2: Reserve a high-VP card
    // -------------------------------------------------------------------------
    
    /**
     * Mid-tier execution policy: Resource denial & strategic planning.
     * Selects and migrates an arbitrarily high-yield (VP >= 2) board asset into 
     * private reserves if capacity (< 3) rules allow. Grants 1 gold collateral. 
     * Triggers strict constraint resolution (auto-discard loops) if injection breaches the 10 token limit.
     */
    private static boolean tryReserveCard(Game game, Player player) {
        if (player.getReservedCards().size() >= 3)
            return false;

        Card best = null;
        int bestSlot = -1;

        List<Card> visible = game.getVisibleCards();
        // O(N) Iteration: Scrape the active board exclusively for high-yield tactical assets.
        // Identifies the card with the highest objective VP independent of current token liquidity.
        for (int i = 0; i < visible.size(); i++) {
            Card c = visible.get(i);
            // Threshold constraint: Reserve action is only financially viable for cards granting >= 2 VP
            if (c != null && c.getValue() >= 2) { 
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
            // Auto-discard if gold coin pushed over 10
            while (player.getTotalCoins() > 10) {
                autoDiscard(game, player);
            }
        }
        return ok;
    }

    /**
     * Corrective boundary constraint algorithm triggering exclusively post-reservation.
     * Computes future deficit vectors targeting the most economically accessible cards, 
     * isolating and aggressively liquidating the holding color causing maximum mathematical zero-waste 'excess'.
     *
     * @param game Scope context.
     * @param player Target executor subject to strict 10-cap rules.
     */
    private static void autoDiscard(Game game, Player player) {
        int[] need = computeNeeds(game, player);
        // Identify the localized color index representing the highest mathematical 'excess'.
        int bestIdx = -1;
        int bestExcess = Integer.MIN_VALUE;
        
        // O(1) Iteration: Compare exact holdings across the strict 6-slot (0-5) token inventory space.
        for (int i = 0; i < 6; i++) {
            // Bypass logic for unheld token variants, preventing negative deficit calculations
            if (player.getMyCoins()[i] <= 0)
                continue;
            // Extrapolate deficit mapping excluding Gold token anomalies (index 5)
            int n = (i < 5) ? need[i] : 0; 
            // Calculate absolute waste gap between possessed stock vs projected structural needs
            int excess = player.getMyCoins()[i] - n;
            // Maximize isolation algorithm
            if (excess > bestExcess) {
                bestExcess = excess;
                bestIdx = i;
            }
        }
        if (bestIdx >= 0) {
            player.discardCoin(GemColor.fromIndex(bestIdx).name());
        }
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

        // O(1) Iteration: Pass 1 - Prioritize tokens strongly aligned with our optimal objective deficit.
        for (int i = 0; i < 5 && selection.size() < remaining; i++) {
            if (need[i] > 0 && bankCoins[i] >= 1) {
                selection.add(GemColor.fromIndex(i).name());
            }
        }

        // O(1) Iteration: Pass 2 - Fallback logic. Scavenge arbitrary available tokens to exhaust the action allowance.
        for (int i = 0; i < 5 && selection.size() < remaining; i++) {
            String name = GemColor.fromIndex(i).name();
            // Restrict duplication constraints maintaining the 3-distinct rule format.
            if (!selection.contains(name) && bankCoins[i] >= 1) {
                selection.add(name);
            }
        }

        // Fallback strategy branch: Action efficiency failsafe. If multi-gem harvesting is starved, 
        // fallback to generating depth by bulk-pulling 2 homogeneous tokens if the bank holds >= 4.
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
                total += Math.max(0, c.getCost()[i] - player.getMyCards()[i] - player.getMyCoins()[i]);
            }
            if (total < minTotalCost) {
                minTotalCost = total;
                target = c;
            }
        }

        if (target != null) {
            for (int i = 0; i < 5; i++) {
                need[i] = Math.max(0, target.getCost()[i] - player.getMyCards()[i] - player.getMyCoins()[i]);
            }
        }
        return need;
    }
}