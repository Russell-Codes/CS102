package com.g1t7.splendor.service;

import com.g1t7.splendor.config.GameConfig;
import com.g1t7.splendor.model.*;
import com.g1t7.splendor.util.CardData;
import com.g1t7.splendor.util.NobleData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GameEngineService {

    @Autowired
    private AIPlayer aiPlayer;

    public void initializeGame(Game game) {
        int numPlayers = Math.max(game.getPlayers().size(), 2);
        initBank(game, numPlayers);
        initDecks(game);
        initBoard(game);
        initNobles(game, numPlayers);
    }

    private void initBank(Game game, int numPlayers) {
        int gemCount = game.getConfig().getGemCount(numPlayers);
        int goldCount = game.getConfig().getGoldCoins();
        game.setBankCoins(new int[] { gemCount, gemCount, gemCount, gemCount, gemCount, goldCount });
    }

    private void initDecks(Game game) {
        List<Card> allCards = CardData.buildDeck(GameConfig.getCardFile());
        for (Card c : allCards) {
            if (c.getTier() == 1)
                game.getTier1Deck().add(c);
            else if (c.getTier() == 2)
                game.getTier2Deck().add(c);
            else
                game.getTier3Deck().add(c);
        }
        Collections.shuffle(game.getTier1Deck());
        Collections.shuffle(game.getTier2Deck());
        Collections.shuffle(game.getTier3Deck());
    }

    private void initBoard(Game game) {
        for (int i = 0; i < Game.TOTAL_VISIBLE_CARDS; i++) {
            game.getVisibleCards().add(null);
        }
        for (int slot = 0; slot < Game.VISIBLE_CARDS_PER_TIER; slot++)
            dealSlot(game, slot);
        for (int slot = Game.VISIBLE_CARDS_PER_TIER; slot < Game.VISIBLE_CARDS_PER_TIER * 2; slot++)
            dealSlot(game, slot);
        for (int slot = Game.VISIBLE_CARDS_PER_TIER * 2; slot < Game.TOTAL_VISIBLE_CARDS; slot++)
            dealSlot(game, slot);
    }

    private void initNobles(Game game, int numPlayers) {
        int nobleCount = game.getConfig().getNobleCount(numPlayers);
        List<Noble> allNobles = NobleData.buildNobles(GameConfig.getNobleFile());
        Collections.shuffle(allNobles);
        game.setActiveNobles(new ArrayList<>(allNobles.subList(0, Math.min(nobleCount, allNobles.size()))));
    }

    private void dealSlot(Game game, int slot) {
        List<Card> deck = deckForSlot(game, slot);
        if (!deck.isEmpty()) {
            game.getVisibleCards().set(slot, deck.remove(deck.size() - 1));
        }
    }

    private List<Card> deckForSlot(Game game, int slot) {
        if (slot < Game.VISIBLE_CARDS_PER_TIER)
            return game.getTier1Deck();
        if (slot < Game.VISIBLE_CARDS_PER_TIER * 2)
            return game.getTier2Deck();
        return game.getTier3Deck();
    }

    public void replenishCard(Game game, int slotIndex) {
        List<Card> deck = deckForSlot(game, slotIndex);
        if (!deck.isEmpty()) {
            game.getVisibleCards().set(slotIndex, deck.remove(deck.size() - 1));
        } else {
            game.getVisibleCards().set(slotIndex, null);
        }
    }

    public void changeTurns(Game game) {
        autoReplenishBoard(game);
        game.setLastActivityTime(System.currentTimeMillis());
        game.setMessage("");

        if (!game.isFinalRound() && game.getCurrentPlayer().getScore() >= game.getConfig().getWinScore()) {
            game.setFinalRound(true);
        }

        int nextTurn = game.getCurrentTurnIndex();
        int attempts = 0;
        do {
            nextTurn = (nextTurn + 1) % game.getPlayers().size();
            attempts++;
        } while (game.getPlayers().get(nextTurn).isEjected() && attempts < game.getPlayers().size());

        // Player 0 is always the starting player
        if (attempts >= game.getPlayers().size() || (game.isFinalRound() && nextTurn == 0)) {
            game.setGameOver(true);
            return;
        }

        game.setCurrentTurnIndex(nextTurn);

        Player next = game.getCurrentPlayer();
        if (next.isAi() && !game.isGameOver()) {
            boolean acted = aiPlayer.takeTurn(game, next);
            if (acted) {
                changeTurns(game);
            }
        }
    }

    public boolean replacePlayerWithAi(Game game, String hostUuid, String targetUuid) {
        if (!game.getHostUuid().equals(hostUuid))
            return false;

        for (Player player : game.getPlayers()) {
            if (player.getUuid() != null && player.getUuid().equals(targetUuid)) {
                player.setAi(true);
                player.setName(player.getName() + " (CPU Replaced)");
                if (game.getCurrentPlayer() == player) {
                    aiPlayer.takeTurn(game, player);
                    changeTurns(game);
                }
                return true;
            }
        }
        return false;
    }

    public boolean ejectPlayer(Game game, String hostUuid, String targetUuid) {
        if (!game.getHostUuid().equals(hostUuid))
            return false;

        for (Player player : game.getPlayers()) {
            if (player.getUuid() != null && player.getUuid().equals(targetUuid)) {
                // Return coins to bank
                for (int i = 0; i < Game.TOTAL_COIN_TYPES; i++) {
                    game.getBankCoins()[i] += player.getCoins()[i];
                    player.getCoins()[i] = 0;
                }
                // Return reserved cards to top of decks
                for (Card c : player.getReservedCards()) {
                    c.setReserved(false);
                    if (c.getTier() == 1)
                        game.getTier1Deck().add(0, c);
                    else if (c.getTier() == 2)
                        game.getTier2Deck().add(0, c);
                    else if (c.getTier() == 3)
                        game.getTier3Deck().add(0, c);
                }
                player.getReservedCards().clear();
                player.setEjected(true);
                player.setName(player.getName() + " (EJECTED)");

                if (game.getCurrentPlayer() == player) {
                    changeTurns(game);
                }
                return true;
            }
        }
        return false;
    }

    private void autoReplenishBoard(Game game) {
        for (int i = 0; i < Game.TOTAL_VISIBLE_CARDS; i++) {
            if (game.getVisibleCards().get(i) == null) {
                replenishCard(game, i);
            }
        }
    }
}