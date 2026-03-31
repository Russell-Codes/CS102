package com.g1t7.splendor;

import com.g1t7.splendor.model.Card;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.GemColor;
import com.g1t7.splendor.model.Noble;
import com.g1t7.splendor.model.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerActionService {

    public boolean buyCard(Game game, Player player, Card card) {
        int[] bankCoins = game.getBankCoins();
        int[] effectiveCost = new int[Player.REGULAR_GEM_TYPES];
        int[] playerCards = player.getMyCards();
        int[] playerCoins = player.getMyCoins();

        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            effectiveCost[i] = Math.max(0, card.getCost()[i] - playerCards[i]);
        }

        int goldNeeded = 0;
        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            int shortfall = Math.max(0, effectiveCost[i] - playerCoins[i]);
            goldNeeded += shortfall;
        }

        // Index 5 is Gold
        if (goldNeeded > playerCoins[Player.REGULAR_GEM_TYPES]) {
            game.setMessage("Not enough coins to buy this card.");
            return false;
        }

        int goldSpent = 0;
        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            int paid = Math.min(playerCoins[i], effectiveCost[i]);
            playerCoins[i] -= paid;
            bankCoins[i] += paid;
            int shortfall = effectiveCost[i] - paid;
            goldSpent += shortfall;
        }
        playerCoins[Player.REGULAR_GEM_TYPES] -= goldSpent;
        bankCoins[Player.REGULAR_GEM_TYPES] += goldSpent;

        player.getCards().push(card);
        playerCards[card.getGemColor().ordinal()]++;
        player.setScore(player.getScore() + card.getValue());
        card.setReserved(false);

        checkNobles(game, player, game.getActiveNobles());
        return true;
    }

    public boolean exchangeCoin(Game game, Player player, List<String> selectedColors) {
        if (selectedColors == null || selectedColors.isEmpty() || selectedColors.size() > 3) {
            game.setMessage("Select 1-3 coins.");
            return false;
        }

        int[] bankCoins = game.getBankCoins();
        int[] toTake = new int[Player.TOTAL_COIN_TYPES];
        int[] playerCoins = player.getMyCoins();

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

        for (int i = 0; i < Player.REGULAR_GEM_TYPES; i++) {
            playerCoins[i] += toTake[i];
            bankCoins[i] -= toTake[i];
        }
        return true;
    }

    public boolean escortCard(Game game, Player player, Card card) {
        if (player.getReservedCards().size() >= 3) {
            game.setMessage("You can only reserve up to 3 cards.");
            return false;
        }
        card.setReserved(true);
        player.getReservedCards().add(card);
        if (game.getBankCoins()[GemColor.GOLD.ordinal()] > 0) {
            player.getMyCoins()[GemColor.GOLD.ordinal()]++;
            game.getBankCoins()[GemColor.GOLD.ordinal()]--;
        }
        return true;
    }

    public void checkNobles(Game game, Player player, List<Noble> nobles) {
        if (nobles == null)
            return;

        List<Noble> satisfied = new ArrayList<>();
        for (Noble noble : nobles) {
            if (noble.isSatisfiedBy(player.getMyCards())) {
                satisfied.add(noble);
            }
        }

        if (satisfied.size() == 1) {
            player.setScore(player.getScore() + satisfied.get(0).getVictoryPoints());
            player.getObtainedNobles().add(satisfied.get(0));
            nobles.remove(satisfied.get(0));
        } else if (satisfied.size() > 1) {
            if (player.isAi()) {
                player.setScore(player.getScore() + satisfied.get(0).getVictoryPoints());
                player.getObtainedNobles().add(satisfied.get(0));
                nobles.remove(satisfied.get(0));
            } else {
                game.setPendingNobleChoice(true);
                game.setPendingNobles(satisfied);
            }
        }
    }

    public boolean discardCoin(Game game, Player player, String color) {
        GemColor gc;
        try {
            gc = GemColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
        int idx = gc.ordinal();
        if (player.getMyCoins()[idx] <= 0)
            return false;

        player.getMyCoins()[idx]--;
        game.getBankCoins()[idx]++;
        return true;
    }
}