package com.g1t7.splendor.service;

import com.g1t7.splendor.model.Card;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.GemColor;
import com.g1t7.splendor.model.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIPlayer {

    private static final int MAX_COIN_LIMIT = Player.MAX_COIN_LIMIT;
    private static final int REGULAR_GEM_TYPES = Player.REGULAR_GEM_TYPES;
    private static final int TOTAL_COIN_TYPES = Player.TOTAL_COIN_TYPES;

    private final PlayerActionService actionService;

    public AIPlayer(PlayerActionService actionService) {
        this.actionService = actionService;
    }

    public boolean takeTurn(Game game, Player player) {
        if (tryBuyBestCard(game, player))
            return true;
        if (tryReserveCard(game, player))
            return true;
        return tryTakeCoins(game, player);
    }

    private boolean tryBuyBestCard(Game game, Player player) {
        Card bestCard = null;
        int bestSlot = -1;
        boolean fromReserve = false;

        List<Card> visible = game.getVisibleCards();
        for (int i = 0; i < visible.size(); i++) {
            Card currentCard = visible.get(i);
            if (currentCard != null && actionService.canAfford(player, currentCard)) {
                if (bestCard == null || currentCard.getValue() > bestCard.getValue()) {
                    bestCard = currentCard;
                    bestSlot = i;
                    fromReserve = false;
                }
            }
        }

        for (int i = 0; i < player.getReservedCards().size(); i++) {
            Card currentCard = player.getReservedCards().get(i);
            if (actionService.canAfford(player, currentCard)) {
                if (bestCard == null || currentCard.getValue() > bestCard.getValue()) {
                    bestCard = currentCard;
                    bestSlot = i;
                    fromReserve = true;
                }
            }
        }

        if (bestCard == null)
            return false;

        boolean isSuccessful = actionService.buyCard(game, player, bestCard);
        if (isSuccessful) {
            if (fromReserve) {
                player.getReservedCards().remove(bestCard);
            } else {
                // REFACTORED: Nullify slot for the Sweeper
                game.getVisibleCards().set(bestSlot, null);
            }
        }
        return isSuccessful;
    }

    private boolean tryReserveCard(Game game, Player player) {
        if (player.getReservedCards().size() >= 3)
            return false;

        Card bestCard = null;
        int bestSlot = -1;

        List<Card> visible = game.getVisibleCards();
        for (int i = 0; i < visible.size(); i++) {
            Card currentCard = visible.get(i);
            if (currentCard != null && currentCard.getValue() >= 2) {
                if (bestCard == null || currentCard.getValue() > bestCard.getValue()) {
                    bestCard = currentCard;
                    bestSlot = i;
                }
            }
        }

        if (bestCard == null)
            return false;

        boolean isSuccessful = actionService.reserveCard(game, player, bestCard);
        if (isSuccessful) {
            // REFACTORED: Nullify slot for the Sweeper
            game.getVisibleCards().set(bestSlot, null);

            while (player.getTotalCoins() > MAX_COIN_LIMIT) {
                autoDiscard(game, player);
            }
        }
        return isSuccessful;
    }

    private void autoDiscard(Game game, Player player) {
        int[] need = computeNeeds(game, player);
        int bestIdx = -1;
        int bestExcess = Integer.MIN_VALUE;

        for (int i = 0; i < TOTAL_COIN_TYPES; i++) {
            if (player.getCoins()[i] <= 0)
                continue;

            int projectedNeed = (i < REGULAR_GEM_TYPES) ? need[i] : 0;
            int excess = player.getCoins()[i] - projectedNeed;

            if (excess > bestExcess) {
                bestExcess = excess;
                bestIdx = i;
            }
        }
        if (bestIdx >= 0) {
            actionService.discardCoin(game, player, GemColor.fromIndex(bestIdx).name());
        }
    }

    private boolean tryTakeCoins(Game game, Player player) {
        if (player.getTotalCoins() >= MAX_COIN_LIMIT)
            return true;

        int[] bankCoins = game.getBankCoins();
        int[] need = computeNeeds(game, player);
        List<String> selection = new ArrayList<>();
        int remaining = Math.min(3, MAX_COIN_LIMIT - player.getTotalCoins());

        for (int i = 0; i < REGULAR_GEM_TYPES && selection.size() < remaining; i++) {
            if (need[i] > 0 && bankCoins[i] >= 1) {
                selection.add(GemColor.fromIndex(i).name());
            }
        }

        for (int i = 0; i < REGULAR_GEM_TYPES && selection.size() < remaining; i++) {
            String name = GemColor.fromIndex(i).name();
            if (!selection.contains(name) && bankCoins[i] >= 1) {
                selection.add(name);
            }
        }

        if (selection.size() <= 1) {
            for (int i = 0; i < REGULAR_GEM_TYPES; i++) {
                if (bankCoins[i] >= 4 && player.getTotalCoins() + 2 <= MAX_COIN_LIMIT) {
                    selection.clear();
                    String name = GemColor.fromIndex(i).name();
                    selection.add(name);
                    selection.add(name);
                    break;
                }
            }
        }

        if (selection.isEmpty())
            return true;

        return actionService.exchangeCoin(game, player, selection);
    }

    private int[] computeNeeds(Game game, Player player) {
        int[] need = new int[REGULAR_GEM_TYPES];
        Card targetCard = null;
        int minTotalCost = Integer.MAX_VALUE;

        for (Card currentCard : game.getVisibleCards()) {
            if (currentCard == null)
                continue;
            int total = 0;
            for (int i = 0; i < REGULAR_GEM_TYPES; i++) {
                total += Math.max(0, currentCard.getCost()[i] - player.getBonuses()[i] - player.getCoins()[i]);
            }
            if (total < minTotalCost) {
                minTotalCost = total;
                targetCard = currentCard;
            }
        }

        if (targetCard != null) {
            for (int i = 0; i < REGULAR_GEM_TYPES; i++) {
                need[i] = Math.max(0, targetCard.getCost()[i] - player.getBonuses()[i] - player.getCoins()[i]);
            }
        }
        return need;
    }
}