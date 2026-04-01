package com.g1t7.splendor.service;

import com.g1t7.splendor.model.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps active game rooms in memory.
 */
@Service
public class GameManager {

    private static final Logger logger = LoggerFactory.getLogger(GameManager.class);
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private static final long ROOM_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes
    private static final long INACTIVITY_THRESHOLD_MS = 60000; // 3 minutes
    private static final long INACTIVITY_WINDOW_MS = 30000; // Exactly matches the sweep rate
    private static final int MAX_LOBBIES = 67;

    private final SimpMessagingTemplate messagingTemplate;

    public GameManager(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Creates a room if the lobby limit has not been reached.
     */
    public String createGame(Game game) {
        if (games.size() >= MAX_LOBBIES) {
            logger.warn("Maximum lobby capacity reached. Cannot create new room.");
            return null;
        }

        String roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        game.setLastActivityTime(System.currentTimeMillis());
        games.put(roomId, game);
        logger.info("🟢 New Room Created: {}", roomId);
        return roomId;
    }

    /**
     * Finds a room by ID (case-insensitive).
     */
    public Game getGame(String roomId) {
        if (roomId == null)
            return null;
        return games.get(roomId.toUpperCase());
    }

    /**
     * Sweeps rooms every 30 seconds.
     * Broadcasts a refresh if the current player hasn't moved in 3 minutes.
     */
    @Scheduled(fixedRate = 30000)
    public void sweepSlowPlayers() {
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Game> entry : games.entrySet()) {
            String roomId = entry.getKey();
            Game game = entry.getValue();

            // Skip if the game is over or hasn't started yet
            if (game.isGameOver() || !game.isStarted())
                continue;

            long timeSinceLastMove = now - game.getLastActivityTime();

            // 3 minutes = 180,000 ms.
            // The 210,000 ms upper bound ensures we only catch them ONCE during a sweep,
            // preventing the clients from getting stuck in an infinite reload loop.
            if (timeSinceLastMove >= INACTIVITY_THRESHOLD_MS - INACTIVITY_WINDOW_MS
                    && timeSinceLastMove < INACTIVITY_THRESHOLD_MS + INACTIVITY_WINDOW_MS) {
                logger.info("⏳ Sweep: Player {} in room {} hasn't moved in 3 minutes. Broadcasting refresh.",
                        game.getCurrentPlayer().getName(), roomId);

                messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
            }
        }
    }

    /**
     * Periodically removes rooms that have been idle too long.
     */
    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    public void cleanupIdleRooms() {
        long now = System.currentTimeMillis();
        games.entrySet().removeIf(entry -> {
            boolean isIdle = (now - entry.getValue().getLastActivityTime()) > ROOM_TIMEOUT_MS;
            if (isIdle)
                logger.info("🔴 Room {} closed due to inactivity.", entry.getKey());
            return isIdle;
        });
    }
}