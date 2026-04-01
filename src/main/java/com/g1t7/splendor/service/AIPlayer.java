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

    // Short aliases for shared constants.
    private static final int MAX_COIN_LIMIT = Player.MAX_COIN_LIMIT;
    private static final int REGULAR_GEM_TYPES = Player.REGULAR_GEM_TYPES;
    private static final int TOTAL_COIN_TYPES = Player.TOTAL_COIN_TYPES;

    private final PlayerActionService actionService;

    public AIPlayer(PlayerActionService actionService) {
        this.actionService = actionService;
    }

    /**
     * Takes one AI turn.
     *
     * @return true if the AI made a move
     */
    public boolean takeTurn(Game game, Player player) {
        if (tryBuyBestCard(game, player))
            return true;
        if (tryReserveCard(game, player))
            return true;
        return tryTakeCoins(game, player);
    }

    /**
     * Tries to buy the highest-value card the AI can currently afford.
     */
    private boolean tryBuyBestCard(Game game, Player player) {
        Card bestCard = null;

        List<Card> visible = game.getVisibleCards();
        for (Card currentCard : visible) {
            if (currentCard != null && actionService.canAfford(player, currentCard)) {
                if (bestCard == null || currentCard.getValue() > bestCard.getValue()) {
                    bestCard = currentCard;
                }
            }
        }

        for (Card currentCard : player.getReservedCards()) {
            if (actionService.canAfford(player, currentCard)) {
                if (bestCard == null || currentCard.getValue() > bestCard.getValue()) {
                    bestCard = currentCard;
                }
            }
        }

        if (bestCard == null)
            return false;

        // PlayerActionService handles board/reserve cleanup.
        return actionService.buyCard(game, player, bestCard);
    }

    /**
     * Tries to reserve a strong visible card when buying is not possible.
     */
    private boolean tryReserveCard(Game game, Player player) {
        if (player.getReservedCards().size() >= 3)
            return false;

        Card bestCard = null;

        List<Card> visible = game.getVisibleCards();
        for (Card currentCard : visible) {
            if (currentCard != null && currentCard.getValue() >= 2) {
                if (bestCard == null || currentCard.getValue() > bestCard.getValue()) {
                    bestCard = currentCard;
                }
            }
        }

        if (bestCard == null)
            return false;

        // PlayerActionService handles board cleanup.
        boolean isSuccessful = actionService.reserveCard(game, player, bestCard);
        if (isSuccessful) {
            while (player.getTotalCoins() > MAX_COIN_LIMIT) {
                autoDiscard(game, player);
            }
        }
        return isSuccessful;
    }

    /**
     * Discards one coin that appears least useful for the AI's current target.
     */
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

    /**
     * Tries to take coins legally, preferring 3 distinct colors needed for a near target card.
     */
    private boolean tryTakeCoins(Game game, Player player) {
        int[] bankCoins = game.getBankCoins();
        int[] need = computeNeeds(game, player);
        List<String> selection = new ArrayList<>();

        // 1. Always attempt to take exactly 3 distinct colors
        for (int i = 0; i < REGULAR_GEM_TYPES && selection.size() < 3; i++) {
            if (need[i] > 0 && bankCoins[i] >= 1) {
                selection.add(GemColor.fromIndex(i).name());
            }
        }

        // Fill up to 3 if we haven't reached it, based on bank availability
        for (int i = 0; i < REGULAR_GEM_TYPES && selection.size() < 3; i++) {
            String name = GemColor.fromIndex(i).name();
            if (!selection.contains(name) && bankCoins[i] >= 1) {
                selection.add(name);
            }
        }

        // 2. If we couldn't get 3 distinct colors, try taking 2 of the same color
        if (selection.size() < 3) {
            selection.clear(); // Taking 1 or 2 distinct colors is illegal
            for (int i = 0; i < REGULAR_GEM_TYPES; i++) {
                if (bankCoins[i] >= 4) {
                    String name = GemColor.fromIndex(i).name();
                    selection.add(name);
                    selection.add(name);
                    break;
                }
            }
        }

        // 3. If neither action is possible, fail the coin take (AI will fall back to
        // reserving a card)
        if (selection.isEmpty()) {
            return false;
        }

        // 4. Process the take and handle end-of-turn discard if over 10 coins
        boolean isSuccessful = actionService.exchangeCoin(game, player, selection);
        if (isSuccessful) {
            while (player.getTotalCoins() > MAX_COIN_LIMIT) {
                autoDiscard(game, player);
            }
        }

        return isSuccessful;
    }

    /**
     * Estimates missing gem counts for the cheapest reachable visible card.
     */
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