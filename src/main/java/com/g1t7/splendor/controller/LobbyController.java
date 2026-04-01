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

@Controller
@RequestMapping("/lobby/{roomId}")
public class LobbyController {

    private final GameManager gameManager;
    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;

    public LobbyController(GameManager gameManager, LobbyService lobbyService, SimpMessagingTemplate messagingTemplate) {
        this.gameManager = gameManager;
        this.lobbyService = lobbyService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping
    public String showLobby(@PathVariable String roomId, Model model, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/?error=notfound";

        String myUuid = (String) session.getAttribute("userUuid");
        Player me = lobbyService.getPlayerByUuid(game, myUuid);

        // Direct URL Hacking Bouncer
        if (me == null && game.getPlayers().size() >= game.getCapacity()) {
            return "redirect:/?error=full";
        }
        if (me == null && game.isStarted()) {
            return "redirect:/?error=started";
        }

        if (game.isStarted())
            return "redirect:/game/" + roomId;

        boolean allReady = game.getPlayers().size() == game.getCapacity() &&
                game.getPlayers().stream().allMatch(Player::isReady);

        model.addAttribute("game", game);
        model.addAttribute("roomId", roomId);
        model.addAttribute("myUuid", myUuid);
        model.addAttribute("me", me);
        model.addAttribute("allReady", allReady);

        return "lobby";
    }

    @PostMapping("/ready")
    public String setReady(@PathVariable String roomId, @RequestParam String playerName, HttpSession session) {
        String myUuid = (String) session.getAttribute("userUuid");
        Game game = gameManager.getGame(roomId);

        // Fail fast if attempting to join a full room via direct POST
        if (game != null && lobbyService.getPlayerByUuid(game, myUuid) == null
                && game.getPlayers().size() >= game.getCapacity()) {
            return "redirect:/lobby/" + roomId + "?error=full";
        }

        if (lobbyService.joinOrUpdatePlayer(roomId, myUuid, playerName)) {
            refreshRoom(roomId);
        }
        return "redirect:/lobby/" + roomId;
    }

    @PostMapping("/add-ai")
    public String addAi(@PathVariable String roomId, HttpSession session) {
        String myUuid = (String) session.getAttribute("userUuid");

        if (lobbyService.addAi(roomId, myUuid)) {
            refreshRoom(roomId);
        }
        return "redirect:/lobby/" + roomId;
    }

    @PostMapping("/remove-ai")
    public String removeAi(@PathVariable String roomId, @RequestParam String targetUuid, HttpSession session) {
        String myUuid = (String) session.getAttribute("userUuid");

        if (lobbyService.removeAi(roomId, myUuid, targetUuid)) {
            refreshRoom(roomId);
        }
        return "redirect:/lobby/" + roomId;
    }

    @PostMapping("/start")
    public String startGame(@PathVariable String roomId, HttpSession session) {
        String myUuid = (String) session.getAttribute("userUuid");

        if (lobbyService.startGame(roomId, myUuid)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/move-player")
    public String movePlayer(@PathVariable String roomId,
            @RequestParam String targetUuid,
            @RequestParam String direction,
            HttpSession session) {
        String myUuid = (String) session.getAttribute("userUuid");

        if (lobbyService.movePlayer(roomId, myUuid, targetUuid, direction)) {
            refreshRoom(roomId);
        }
        return "redirect:/lobby/" + roomId;
    }

    /**
     * Helper method to broadcast a websocket refresh command.
     */
    private void refreshRoom(String roomId) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
    }

}