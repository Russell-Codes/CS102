package com.g1t7.splendor;

import com.g1t7.splendor.model.Game;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameManager {
    
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private static final long ROOM_TIMEOUT_MS = 2 * 60 * 60 * 1000; 

    public String createGame(Game game) {
        String roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        game.setLastActivityTime(System.currentTimeMillis()); 
        games.put(roomId, game);
        System.out.println("🟢 New Room Created: " + roomId);
        return roomId;
    }

    public Game getGame(String roomId) {
        if (roomId == null) return null;
        return games.get(roomId.toUpperCase());
    }

    @Scheduled(fixedRate = 1800000)
    public void cleanupIdleRooms() {
        long now = System.currentTimeMillis();
        games.entrySet().removeIf(entry -> {
            boolean isIdle = (now - entry.getValue().getLastActivityTime()) > ROOM_TIMEOUT_MS;
            if (isIdle) System.out.println("🔴 Room " + entry.getKey() + " closed due to inactivity.");
            return isIdle;
        });
    }
}