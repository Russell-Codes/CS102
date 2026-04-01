package com.g1t7.splendor.service;

import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for managing lobby state before a game begins.
 * Handles player joining, AI management, readiness, and game initialization.
 */
@Service
public class LobbyService {

    @Autowired
    private GameManager gameManager;

    @Autowired
    private GameEngineService gameEngineService;

    /**
     * Adds a human player to the lobby or updates their existing name, and sets
     * them to ready.
     *
     * @param roomId     The ID of the room.
     * @param userUuid   The UUID of the joining user.
     * @param playerName The display name provided by the user.
     * @return true if the player was successfully added or updated; false if the
     *         room is full or started.
     */
    public boolean joinOrUpdatePlayer(String roomId, String userUuid, String playerName) {
        Game game = gameManager.getGame(roomId);
        if (game == null || game.isStarted())
            return false;

        Player me = getPlayerByUuid(game, userUuid);

        if (me == null) {
            if (game.getPlayers().size() < game.getCapacity()) {
                me = new Player(playerName.trim());
                me.setUuid(userUuid);
                game.getPlayers().add(me);
            } else {
                return false; // Room is full
            }
        } else {
            me.setName(playerName.trim());
        }

        me.setReady(true);
        return true;
    }

    /**
     * Adds an AI player to the room, provided the requester is the host and the
     * room has space.
     *
     * @param roomId   The ID of the room.
     * @param hostUuid The UUID of the user requesting the AI addition.
     * @return true if the AI was successfully added.
     */
    public boolean addAi(String roomId, String hostUuid) {
        Game game = gameManager.getGame(roomId);
        if (game != null && game.getHostUuid().equals(hostUuid) && game.getPlayers().size() < game.getCapacity()) {
            int aiCount = (int) game.getPlayers().stream().filter(Player::isAi).count() + 1;

            Player aiPlayer = new Player("CPU " + aiCount, true);
            aiPlayer.setUuid(UUID.randomUUID().toString());
            aiPlayer.setReady(true);

            game.getPlayers().add(aiPlayer);
            return true;
        }
        return false;
    }

    /**
     * Removes an AI player from the room, provided the requester is the host.
     *
     * @param roomId     The ID of the room.
     * @param hostUuid   The UUID of the user requesting the removal.
     * @param targetUuid The UUID of the AI player to remove.
     * @return true if the AI was successfully removed.
     */
    public boolean removeAi(String roomId, String hostUuid, String targetUuid) {
        Game game = gameManager.getGame(roomId);
        if (game != null && game.getHostUuid().equals(hostUuid)) {
            return game.getPlayers().removeIf(p -> p.isAi() && targetUuid.equals(p.getUuid()));
        }
        return false;
    }

    /**
     * Starts the game, provided the requester is the host, the room is full, and
     * all players are ready.
     *
     * @param roomId   The ID of the room.
     * @param hostUuid The UUID of the user requesting the game start.
     * @return true if the game was successfully initialized and started.
     */
    public boolean startGame(String roomId, String hostUuid) {
        Game game = gameManager.getGame(roomId);
        if (game != null && game.getHostUuid().equals(hostUuid) && game.getPlayers().size() == game.getCapacity()) {
            boolean allReady = game.getPlayers().stream().allMatch(Player::isReady);
            if (allReady) {
                game.setStarted(true);
                gameEngineService.initializeGame(game);
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to locate a player within a game by their UUID.
     */
    public Player getPlayerByUuid(Game game, String uuid) {
        if (game == null || uuid == null)
            return null;
        return game.getPlayers().stream()
                .filter(p -> uuid.equals(p.getUuid()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Initializes a new game room, sets the capacity, and adds the host player.
     *
     * @param numPlayers The maximum number of players allowed in the room.
     * @param hostName   The display name of the host.
     * @param hostUuid   The unique identifier for the host's session.
     * @return The generated Room ID.
     */
    public String createNewRoom(int numPlayers, String hostName, String hostUuid) {
        Game game = new Game();
        game.setCapacity(Math.max(2, Math.min(4, numPlayers)));
        game.setStarted(false);
        game.setHostUuid(hostUuid);

        Player host = new Player(hostName.trim());
        host.setUuid(hostUuid);
        host.setReady(true);
        game.getPlayers().add(host);

        return gameManager.createGame(game);
    }

    /**
     * Moves a player up or down in the play order.
     */
    public boolean movePlayer(String roomId, String hostUuid, String targetUuid, String direction) {
        Game game = gameManager.getGame(roomId);

        // Only the host can reorder, and only before the game starts
        if (game == null || !game.getHostUuid().equals(hostUuid) || game.isStarted()) {
            return false;
        }

        List<Player> players = game.getPlayers();
        int index = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUuid() != null && players.get(i).getUuid().equals(targetUuid)) {
                index = i;
                break;
            }
        }

        if (index == -1)
            return false;

        // Swap with the player above
        if ("up".equalsIgnoreCase(direction) && index > 0) {
            Player temp = players.get(index - 1);
            players.set(index - 1, players.get(index));
            players.set(index, temp);
            return true;
        }
        // Swap with the player below
        else if ("down".equalsIgnoreCase(direction) && index < players.size() - 1) {
            Player temp = players.get(index + 1);
            players.set(index + 1, players.get(index));
            players.set(index, temp);
            return true;
        }

        return false;
    }
}