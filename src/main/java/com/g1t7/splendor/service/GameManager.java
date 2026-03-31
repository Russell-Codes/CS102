package com.g1t7.splendor.service;

import com.g1t7.splendor.model.Card;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameManager {

    private static final Logger logger = LoggerFactory.getLogger(GameManager.class);
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private static final long ROOM_TIMEOUT_MS = 2 * 60 * 60 * 1000;

    @Autowired
    private GameEngineService gameEngineService;

    @Autowired
    private AIPlayer aiPlayer;

    public String createGame(Game game) {
        String roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        game.setLastActivityTime(System.currentTimeMillis());
        games.put(roomId, game);
        logger.info("🟢 New Room Created: {}", roomId);
        return roomId;
    }

    public Game getGame(String roomId) {
        if (roomId == null)
            return null;
        return games.get(roomId.toUpperCase());
    }

    @Scheduled(fixedRate = 1800000)
    public void cleanupIdleRooms() {
        long now = System.currentTimeMillis();
        games.entrySet().removeIf(entry -> {
            boolean isIdle = (now - entry.getValue().getLastActivityTime()) > ROOM_TIMEOUT_MS;
            if (isIdle)
                logger.info("🔴 Room {} closed due to inactivity.", entry.getKey());
            return isIdle;
        });
    }

    public boolean replacePlayerWithAi(String roomId, String hostUuid, String targetUuid) {
        Game game = getGame(roomId);
        if (game == null || !game.getHostUuid().equals(hostUuid))
            return false;

        for (Player player : game.getPlayers()) {
            if (player.getUuid() != null && player.getUuid().equals(targetUuid)) {
                player.setAi(true);
                player.setName(player.getName() + " (CPU Replaced)");
                if (game.getCurrentPlayer() == player) {
                    aiPlayer.takeTurn(game, player);
                    gameEngineService.changeTurns(game);
                }
                return true;
            }
        }
        return false;
    }

    public boolean ejectPlayer(String roomId, String hostUuid, String targetUuid) {
        Game game = getGame(roomId);
        if (game == null || !game.getHostUuid().equals(hostUuid))
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
                    gameEngineService.changeTurns(game);
                }
                return true;
            }
        }
        return false;
    }
}