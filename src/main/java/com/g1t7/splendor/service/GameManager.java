package com.g1t7.splendor.service;

import com.g1t7.splendor.model.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameManager {

    private static final Logger logger = LoggerFactory.getLogger(GameManager.class);
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private static final long ROOM_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes
    public static final int MAX_LOBBIES = 50;

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
}