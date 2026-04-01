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
 * Runs core player actions like buying cards, taking coins, and reserving.
 */
@Service
public class PlayerActionService {

    /**
     * Buys a card for a player if they can afford it.
     */
    public boolean buyCard(Game game, Player player, Card card) {
        if (!canAfford(player, card)) {
            game.setMessage("Not enough coins to buy this card.");
            return false;
        }

        int[] bankCoins = game.getBankCoins();
        int[] effectiveCost = new int[Player.REGULAR_GEM_TYPES];
        int[] playerCards = player.getBonuses();
        int[] playerCoins = player.getCoins();
        int goldIndex = GemColor.GOLD.ordinal();

        // Calculate cost after applying card discounts
        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            effectiveCost[i] = Math.max(0, card.getCost()[i] - playerCards[i]);
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

        // Remove the card from wherever it came from.
        if (card.isReserved()) {
            player.getReservedCards().remove(card);
            card.setReserved(false);
        } else {
            int index = game.getVisibleCards().indexOf(card);
            if (index != -1) {
                game.getVisibleCards().set(index, null);
            }
        }

        checkNobles(game, player, game.getActiveNobles());

        return true;
    }

    /**
     * Validates and applies a coin-take action.
     *
     * @param game           current game state
     * @param player         current player
     * @param selectedColors chosen colors
     * @return true if the exchange is legal and processed; false otherwise.
     */
    public boolean exchangeCoin(Game game, Player player, List<String> selectedColors) {
        if (selectedColors == null || selectedColors.size() < 2 || selectedColors.size() > 3) {
            game.setMessage("You must take exactly 3 distinct coins or 2 of the same color.");
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

        // Splendor take rules
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
     * Reserves a card and gives one gold coin if available.
     */
    public boolean reserveCard(Game game, Player player, Card card) {
        if (player.getReservedCards().size() >= 3) {
            game.setMessage("You can only reserve up to 3 cards.");
            return false;
        }

        card.setReserved(true);
        player.getReservedCards().add(card);

        // Remove the reserved card from the board if it is visible.
        int index = game.getVisibleCards().indexOf(card);
        if (index != -1) {
            game.getVisibleCards().set(index, null);
        }

        int goldIndex = GemColor.GOLD.ordinal();
        if (game.getBankCoins()[goldIndex] > 0) {
            player.getCoins()[goldIndex]++;
            game.getBankCoins()[goldIndex]--;
        }
        return true;
    }

    /**
     * Checks whether one or more nobles can be claimed.
     *
     * @param game   current game state
     * @param player player being checked
     * @param nobles nobles currently on the board
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
                // AI picks the first available noble.
                player.setScore(player.getScore() + satisfied.get(0).getVictoryPoints());
                player.getObtainedNobles().add(satisfied.get(0));
                nobles.remove(satisfied.get(0));
            } else {
                // Human player chooses on the next step.
                game.setPendingNobleChoice(true);
                game.setPendingNobles(satisfied);
            }
        }
    }

    /**
     * Discards one coin back to the bank.
     *
     * @param game   current game state
     * @param player player discarding
     * @param color  color name
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

    /**
     * Checks if a player can pay for a card using coins and gold.
     */
    public boolean canAfford(Player player, Card card) {
        int goldNeeded = 0;
        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            int effectiveCost = Math.max(0, card.getCost()[i] - player.getBonuses()[i]);
            int shortfall = Math.max(0, effectiveCost - player.getCoins()[i]);
            goldNeeded += shortfall;
        }
        return goldNeeded <= player.getCoins()[GemColor.GOLD.ordinal()];
    }
}