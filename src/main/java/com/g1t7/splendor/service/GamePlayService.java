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
 * Handles player actions that happen during a turn.
 */
@Service
public class GamePlayService {

    private GameManager gameManager;
    private PlayerActionService playerActionService;
    private GameEngineService gameEngineService;

    public GamePlayService(GameManager gameManager, PlayerActionService playerActionService,
            GameEngineService gameEngineService) {
        this.gameManager = gameManager;
        this.playerActionService = playerActionService;
        this.gameEngineService = gameEngineService;
    }

    /**
     * Handles a coin-take request for the current player.
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
     * Handles a buy-card request.
     */
    public boolean buyCard(String roomId, String userUuid, int cardIndex) {
        Game game = fetchActiveGameAndValidateTurn(roomId, userUuid);
        if (game == null || game.isPendingDiscard() || game.isPendingNobleChoice())
            return false;

        Player current = game.getCurrentPlayer();
        Card card = resolveCard(game, current, cardIndex);

        if (card != null && playerActionService.buyCard(game, current, card)) {
            if (cardIndex >= 0) {
                game.getVisibleCards().set(cardIndex, null);
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
     * Handles noble selection when multiple nobles are available.
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
     * Handles a reserve-card request.
     */
    public boolean reserveCard(String roomId, String userUuid, int cardIndex) {
        Game game = fetchActiveGameAndValidateTurn(roomId, userUuid);
        if (game == null || game.isPendingDiscard() || game.isPendingNobleChoice())
            return false;

        if (cardIndex >= 0 && cardIndex < game.getVisibleCards().size()) {
            Player current = game.getCurrentPlayer();
            Card card = game.getVisibleCards().get(cardIndex);

            if (card != null && playerActionService.reserveCard(game, current, card)) {
                game.getVisibleCards().set(cardIndex, null);

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
     * Handles a single discard while the player is over the coin limit.
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

    // Build a flat list of color names from incoming counts.
    private List<String> buildColorList(int[] counts) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            String colorName = GemColor.fromIndex(i).name();
            for (int j = 0; j < Math.min(Math.max(counts[i], 0), 2); j++) {
                list.add(colorName);
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