package com.g1t7.splendor.service;

import com.g1t7.splendor.model.Card;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.GemColor;
import com.g1t7.splendor.model.Noble;
import com.g1t7.splendor.model.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for executing core player actions.
 * Handles the logic for buying cards, exchanging coins, reserving cards,
 * and checking for noble visits.
 */
@Service
public class PlayerActionService {

    /**
     * Attempts to purchase a card for a player.
     * Calculates the effective cost (discounted by the player's current cards)
     * and automatically applies Gold wildcard coins if needed.
     *
     * @param game   The current game state.
     * @param player The player attempting the purchase.
     * @param card   The card being purchased.
     * @return true if the transaction was successful; false if funds are
     *         insufficient.
     */
    public boolean buyCard(Game game, Player player, Card card) {
        int[] bankCoins = game.getBankCoins();
        int[] effectiveCost = new int[Player.REGULAR_GEM_TYPES];
        int[] playerCards = player.getBonuses();
        int[] playerCoins = player.getCoins();
        int goldIndex = GemColor.GOLD.ordinal();

        // Calculate cost after applying card discounts
        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            effectiveCost[i] = Math.max(0, card.getCost()[i] - playerCards[i]);
        }

        // Calculate how much wildcard Gold is required to cover shortfalls
        int goldNeeded = 0;
        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            int shortfall = Math.max(0, effectiveCost[i] - playerCoins[i]);
            goldNeeded += shortfall;
        }

        if (goldNeeded > playerCoins[goldIndex]) {
            game.setMessage("Not enough coins to buy this card.");
            return false;
        }

        // Process the payment
        int goldSpent = 0;
        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            int paid = Math.min(playerCoins[i], effectiveCost[i]);
            playerCoins[i] -= paid;
            bankCoins[i] += paid;
            int shortfall = effectiveCost[i] - paid;
            goldSpent += shortfall;
        }

        playerCoins[goldIndex] -= goldSpent;
        bankCoins[goldIndex] += goldSpent;

        // Grant the card to the player
        player.getCards().add(card);
        playerCards[card.getGemColor().ordinal()]++;
        player.setScore(player.getScore() + card.getValue());
        card.setReserved(false);
        checkNobles(game, player, game.getActiveNobles());
        return true;
    }

    /**
     * Validates and processes a player's request to take gems from the bank.
     * Enforces the rules: Take 3 distinct colors, OR take 2 of the same color
     * (only if the bank has at least 4 of that color).
     *
     * @param game           The current game state.
     * @param player         The player taking the coins.
     * @param selectedColors List of string representations of the chosen colors.
     * @return true if the exchange is legal and processed; false otherwise.
     */
    public boolean exchangeCoin(Game game, Player player, List<String> selectedColors) {
        if (selectedColors == null || selectedColors.isEmpty() || selectedColors.size() > 3) {
            game.setMessage("Select 1-3 coins.");
            return false;
        }

        int[] bankCoins = game.getBankCoins();
        int[] toTake = new int[Player.TOTAL_COIN_TYPES];
        int[] playerCoins = player.getCoins();

        // Parse requested colors
        for (String colorName : selectedColors) {
            GemColor gemColor;
            try {
                gemColor = GemColor.valueOf(colorName.toUpperCase());
            } catch (IllegalArgumentException e) {
                game.setMessage("Invalid gem color.");
                return false;
            }
            if (gemColor == GemColor.GOLD) {
                game.setMessage("Cannot take gold coins directly.");
                return false;
            }
            toTake[gemColor.ordinal()]++;
        }

        // Validate the specific Splendor taking rules
        if (selectedColors.size() == 2) {
            int distinctCount = 0;
            int sameColorIdx = -1;
            for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
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
            for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
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

        // Execute the coin transfer
        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            playerCoins[i] += toTake[i];
            bankCoins[i] -= toTake[i];
        }
        return true;
    }

    /**
     * Reserves a card for the player and grants them 1 Gold wildcard (if
     * available).
     *
     * @param game   The current game state.
     * @param player The player reserving the card.
     * @param card   The card to be reserved.
     * @return true if successful; false if the player already has 3 reserved cards.
     */
    public boolean reserveCard(Game game, Player player, Card card) {
        if (player.getReservedCards().size() >= 3) {
            game.setMessage("You can only reserve up to 3 cards.");
            return false;
        }

        card.setReserved(true);
        player.getReservedCards().add(card);

        int goldIndex = GemColor.GOLD.ordinal();
        if (game.getBankCoins()[goldIndex] > 0) {
            player.getCoins()[goldIndex]++;
            game.getBankCoins()[goldIndex]--;
        }
        return true;
    }

    /**
     * Evaluates if a player has met the requirements to be visited by any Nobles.
     * If multiple are satisfied, flags the game state to await the player's choice.
     *
     * @param game   The current game state.
     * @param player The player being evaluated.
     * @param nobles The list of currently available nobles on the board.
     */
    public void checkNobles(Game game, Player player, List<Noble> nobles) {
        if (nobles == null)
            return;

        List<Noble> satisfied = new ArrayList<>();
        for (Noble noble : nobles) {
            if (noble.isSatisfiedBy(player.getBonuses())) {
                satisfied.add(noble);
            }
        }

        if (satisfied.size() == 1) {
            player.setScore(player.getScore() + satisfied.get(0).getVictoryPoints());
            player.getObtainedNobles().add(satisfied.get(0));
            nobles.remove(satisfied.get(0));
        } else if (satisfied.size() > 1) {
            if (player.isAi()) {
                // AI automatically takes the first satisfied noble
                player.setScore(player.getScore() + satisfied.get(0).getVictoryPoints());
                player.getObtainedNobles().add(satisfied.get(0));
                nobles.remove(satisfied.get(0));
            } else {
                // Human player must choose
                game.setPendingNobleChoice(true);
                game.setPendingNobles(satisfied);
            }
        }
    }

    /**
     * Discards a single coin from the player's inventory back to the bank.
     *
     * @param game   The current game state.
     * @param player The player discarding the coin.
     * @param color  The string representation of the color being discarded.
     * @return true if successful; false if the color is invalid or the player has
     *         none.
     */
    public boolean discardCoin(Game game, Player player, String color) {
        GemColor gemColor;
        try {
            gemColor = GemColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }

        int idx = gemColor.ordinal();
        if (player.getCoins()[idx] <= 0)
            return false;

        player.getCoins()[idx]--;
        game.getBankCoins()[idx]++;
        return true;
    }
}