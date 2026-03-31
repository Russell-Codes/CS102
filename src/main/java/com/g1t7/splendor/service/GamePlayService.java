package com.g1t7.splendor.service;

import com.g1t7.splendor.model.Card;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Noble;
import com.g1t7.splendor.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for orchestrating gameplay actions.
 * Acts as the bridge between player requests and core game logic.
 */
@Service
public class GamePlayService {

    @Autowired
    private GameManager gameManager;
    @Autowired
    private PlayerActionService playerActionService;
    @Autowired
    private GameEngineService gameEngineService;

    /**
     * Processes a player's request to take gems from the bank.
     *
     * @param roomId   The ID of the game room.
     * @param userUuid The UUID of the requesting player.
     * @param counts   An array of requested gem counts [White, Blue, Green, Red,
     *                 Black].
     * @return true if the action was successful and the board needs refreshing.
     */
    public boolean takeCoins(String roomId, String userUuid, int[] counts) {
        Game game = fetchActiveGameAndValidateTurn(roomId, userUuid);
        if (game == null || game.isPendingDiscard() || game.isPendingNobleChoice())
            return false;

        List<String> selectedColors = buildColorList(counts);
        if (selectedColors.isEmpty())
            return false;

        Player current = game.getCurrentPlayer();
        if (playerActionService.exchangeCoin(game, current, selectedColors)) {
            if (current.getTotalCoins() > Player.MAX_COIN_LIMIT) {
                game.setPendingDiscard(true);
            } else {
                gameEngineService.changeTurns(game);
            }
            return true;
        }
        return false;
    }

    /**
     * Processes a player's request to buy a card from the board or their reserves.
     */
    public boolean buyCard(String roomId, String userUuid, int cardIndex) {
        Game game = fetchActiveGameAndValidateTurn(roomId, userUuid);
        if (game == null || game.isPendingDiscard() || game.isPendingNobleChoice())
            return false;

        Player current = game.getCurrentPlayer();
        Card card = resolveCard(game, current, cardIndex);

        if (card != null && playerActionService.buyCard(game, current, card)) {
            if (cardIndex >= 0) {
                gameEngineService.replenishCard(game, cardIndex);
            } else {
                current.getReservedCards().remove(card);
            }

            if (!game.isPendingNobleChoice()) {
                gameEngineService.changeTurns(game);
            }
            return true;
        }
        return false;
    }

    /**
     * Processes a player's choice of Noble when multiple are earned at once.
     */
    public boolean claimNoble(String roomId, String userUuid, int nobleIndex) {
        Game game = fetchActiveGameAndValidateTurn(roomId, userUuid);
        if (game == null || !game.isPendingNobleChoice())
            return false;

        if (nobleIndex >= 0 && nobleIndex < game.getPendingNobles().size()) {
            Player current = game.getCurrentPlayer();
            Noble chosen = game.getPendingNobles().get(nobleIndex);

            current.setScore(current.getScore() + chosen.getVictoryPoints());
            current.getObtainedNobles().add(chosen);

            game.getActiveNobles().remove(chosen);
            game.setPendingNobleChoice(false);
            game.getPendingNobles().clear();

            gameEngineService.changeTurns(game);
            return true;
        }
        return false;
    }

    /**
     * Processes a player's request to reserve a visible card.
     */
    public boolean reserveCard(String roomId, String userUuid, int cardIndex) {
        Game game = fetchActiveGameAndValidateTurn(roomId, userUuid);
        if (game == null || game.isPendingDiscard() || game.isPendingNobleChoice())
            return false;

        if (cardIndex >= 0 && cardIndex < game.getVisibleCards().size()) {
            Player current = game.getCurrentPlayer();
            Card card = game.getVisibleCards().get(cardIndex);

            if (card != null && playerActionService.reserveCard(game, current, card)) {
                gameEngineService.replenishCard(game, cardIndex);
                if (current.getTotalCoins() > Player.MAX_COIN_LIMIT) {
                    game.setPendingDiscard(true);
                } else {
                    gameEngineService.changeTurns(game);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Processes a player's required coin discard when they exceed the 10-coin
     * limit.
     */
    public boolean discardCoins(String roomId, String userUuid, String color) {
        Game game = fetchActiveGameAndValidateTurn(roomId, userUuid);
        if (game == null || !game.isPendingDiscard() || game.isPendingNobleChoice())
            return false;

        Player current = game.getCurrentPlayer();
        boolean isDiscardSuccessful = playerActionService.discardCoin(game, current, color);

        if (isDiscardSuccessful && current.getTotalCoins() <= Player.MAX_COIN_LIMIT) {
            game.setPendingDiscard(false);
            gameEngineService.changeTurns(game);
        }
        return isDiscardSuccessful;
    }

    // --- Helper Methods ---

    private Game fetchActiveGameAndValidateTurn(String roomId, String userUuid) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return null;
        Player current = game.getCurrentPlayer();
        if (!current.getUuid().equals(userUuid))
            return null;
        return game;
    }

    private List<String> buildColorList(int[] counts) {
        List<String> list = new ArrayList<>();
        String[] colors = { "WHITE", "BLUE", "GREEN", "RED", "BLACK" };
        for (int i = 0; i < counts.length; i++) {
            for (int j = 0; j < Math.min(Math.max(counts[i], 0), 2); j++) {
                list.add(colors[i]);
            }
        }
        return list;
    }

    private Card resolveCard(Game game, Player player, int cardIndex) {
        if (cardIndex >= 0 && cardIndex < game.getVisibleCards().size()) {
            return game.getVisibleCards().get(cardIndex);
        }
        int reservedIdx = -(cardIndex + 1);
        if (reservedIdx >= 0 && reservedIdx < player.getReservedCards().size()) {
            return player.getReservedCards().get(reservedIdx);
        }
        return null;
    }
}