package com.g1t7.splendor.controller;

import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Player;
import com.g1t7.splendor.service.GameManager;
import com.g1t7.splendor.service.LobbyService;

import jakarta.servlet.http.HttpSession;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Handles lobby actions before the game starts.
 */
@Controller
@RequestMapping("/lobby/{roomId}")
public class LobbyController {

    private final GameManager gameManager;
    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;

    public LobbyController(GameManager gameManager, LobbyService lobbyService,
            SimpMessagingTemplate messagingTemplate) {
        this.gameManager = gameManager;
        this.lobbyService = lobbyService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Shows the lobby page if access checks pass.
     */
    @GetMapping
    public String showLobby(@PathVariable String roomId, Model model, HttpSession session) {
        // Look up the room
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/?error=notfound";

        // Get current player's UUID from session
        String myUuid = (String) session.getAttribute("userUuid");
        // Check if this player is already in the room
        Player me = lobbyService.getPlayerByUuid(game, myUuid);

        // Prevent joining by URL if the room is already full or started.
        if (me == null && game.getPlayers().size() >= game.getCapacity()) {
            return "redirect:/?error=full";
        }
        // Prevent joining a game that's already in progress
        if (me == null && game.isStarted()) {
            return "redirect:/?error=started";
        }

        // If game has started, show game page instead of lobby
        if (game.isStarted())
            return "redirect:/game/" + roomId;

        // Check if all players are ready: room is full AND all players marked as ready
        boolean allReady = game.getPlayers().size() == game.getCapacity() &&
                game.getPlayers().stream().allMatch(Player::isReady);

        model.addAttribute("game", game);
        model.addAttribute("roomId", roomId);
        model.addAttribute("myUuid", myUuid);
        model.addAttribute("me", me);
        model.addAttribute("allReady", allReady);

        return "lobby";
    }

    /**
     * Join/update player info and mark the player as ready.
     */
    @PostMapping("/ready")
    public String setReady(@PathVariable String roomId, @RequestParam String playerName, HttpSession session) {
        String myUuid = (String) session.getAttribute("userUuid");
        Game game = gameManager.getGame(roomId);

        // Block direct POST joins when room is already full.
        if (game != null && lobbyService.getPlayerByUuid(game, myUuid) == null
                && game.getPlayers().size() >= game.getCapacity()) {
            return "redirect:/lobby/" + roomId + "?error=full";
        }

        // Add or update player in the room and mark as ready
        if (lobbyService.joinOrUpdatePlayer(roomId, myUuid, playerName)) {
            refreshRoom(roomId);
        }
        return "redirect:/lobby/" + roomId;
    }

    /**
     * Host action: add an AI player.
     */
    @PostMapping("/add-ai")
    public String addAi(@PathVariable String roomId, HttpSession session) {
        String myUuid = (String) session.getAttribute("userUuid");

        // Attempt to add AI player (fails if not host or room full)
        if (lobbyService.addAi(roomId, myUuid)) {
            refreshRoom(roomId);
        }
        return "redirect:/lobby/" + roomId;
    }

    /**
     * Host action: remove an AI player.
     */
    @PostMapping("/remove-ai")
    public String removeAi(@PathVariable String roomId, @RequestParam String targetUuid, HttpSession session) {
        String myUuid = (String) session.getAttribute("userUuid");

        // Attempt to remove AI player by UUID (fails if not host or not an AI)
        if (lobbyService.removeAi(roomId, myUuid, targetUuid)) {
            refreshRoom(roomId);
        }
        return "redirect:/lobby/" + roomId;
    }

    /**
     * Host action: start the game.
     */
    @PostMapping("/start")
    public String startGame(@PathVariable String roomId, HttpSession session) {
        String myUuid = (String) session.getAttribute("userUuid");

        // Attempt to start game (fails if not host, room not full, or not all ready)
        if (lobbyService.startGame(roomId, myUuid)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    /**
     * Host action: move a player up or down before the game starts.
     */
    @PostMapping("/move-player")
    public String movePlayer(@PathVariable String roomId,
            @RequestParam String targetUuid,
            @RequestParam String direction,
            HttpSession session) {
        String myUuid = (String) session.getAttribute("userUuid");

        // Attempt to move player (fails if not host, game started, or invalid
        // direction)
        if (lobbyService.movePlayer(roomId, myUuid, targetUuid, direction)) {
            refreshRoom(roomId);
        }
        return "redirect:/lobby/" + roomId;
    }

    /**
     * Sends a refresh event to all clients in the room.
     */
    private void refreshRoom(String roomId) {
        // Broadcast REFRESH message to all WebSocket subscribers on this room's topic
        messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
    }

}