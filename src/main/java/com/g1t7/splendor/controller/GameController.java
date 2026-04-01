package com.g1t7.splendor.controller;

import com.g1t7.splendor.service.GameManager;
import com.g1t7.splendor.service.GameEngineService;
import com.g1t7.splendor.service.GamePlayService;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Handles game actions for a room.
 */
@Controller
@RequestMapping("/game/{roomId}")
public class GameController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameManager gameManager;
    private final GamePlayService gamePlayService;
    private final GameEngineService gameEngineService;

    public GameController(SimpMessagingTemplate messagingTemplate, GameManager gameManager,
            GamePlayService gamePlayService, GameEngineService gameEngineService) {
        this.messagingTemplate = messagingTemplate;
        this.gameManager = gameManager;
        this.gamePlayService = gamePlayService;
        this.gameEngineService = gameEngineService;
    }

    /**
        * Shows the game page if the room is valid and started.
     */
    @GetMapping
    public String showGame(@PathVariable String roomId, Model model, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null || !game.isStarted())
            return "redirect:/";
        if (game.isGameOver())
            return "redirect:/gameover/" + roomId;

        model.addAttribute("game", game);
        model.addAttribute("roomId", roomId);
        model.addAttribute("myUuid", session.getAttribute("userUuid"));
        return "game";
    }

    /**
        * Heartbeat endpoint so the server knows a player is still connected.
     */
    @PostMapping("/ping")
    @ResponseBody
    public String handlePing(@PathVariable String roomId, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        String myUuid = (String) session.getAttribute("userUuid");
        if (game != null && myUuid != null) {
            for (Player player : game.getPlayers()) {
                if (myUuid.equals(player.getUuid())) {
                    player.setLastHeartbeat(System.currentTimeMillis());
                    break;
                }
            }
        }
        return "ok";
    }

    /**
        * Host action: replace a player with AI.
     */
    @PostMapping("/host-action/ai")
    public String replaceWithAi(@PathVariable String roomId, @RequestParam String targetUuid, HttpSession session) {
        String sessionUuid = (String) session.getAttribute("userUuid");
        Game game = gameManager.getGame(roomId);

        if (game != null && gameEngineService.replacePlayerWithAi(game, sessionUuid, targetUuid)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    /**
        * Host action: eject a player.
     */
    @PostMapping("/host-action/eject")
    public String ejectPlayer(@PathVariable String roomId, @RequestParam String targetUuid, HttpSession session) {
        String sessionUuid = (String) session.getAttribute("userUuid");
        Game game = gameManager.getGame(roomId);

        if (game != null && gameEngineService.ejectPlayer(game, sessionUuid, targetUuid)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    /**
        * Current player takes coins.
     */
    @PostMapping("/take-coins")
    public String takeCoins(@PathVariable String roomId,
            @RequestParam(defaultValue = "0") int white,
            @RequestParam(defaultValue = "0") int blue,
            @RequestParam(defaultValue = "0") int green,
            @RequestParam(defaultValue = "0") int red,
            @RequestParam(defaultValue = "0") int black,
            HttpSession session) {
        String userUuid = (String) session.getAttribute("userUuid");
        int[] counts = { white, blue, green, red, black };

        if (gamePlayService.takeCoins(roomId, userUuid, counts)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    /**
        * Current player buys a card.
     */
    @PostMapping("/buy-card")
    public String buyCard(@PathVariable String roomId, @RequestParam("cardIndex") int cardIndex, HttpSession session) {
        String userUuid = (String) session.getAttribute("userUuid");

        if (gamePlayService.buyCard(roomId, userUuid, cardIndex)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    /**
        * Current player claims one of the pending nobles.
     */
    @PostMapping("/claim-noble")
    public String claimNoble(@PathVariable String roomId, @RequestParam("nobleIndex") int nobleIndex,
            HttpSession session) {
        String userUuid = (String) session.getAttribute("userUuid");

        if (gamePlayService.claimNoble(roomId, userUuid, nobleIndex)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    /**
        * Current player reserves a visible card.
     */
    @PostMapping("/reserve-card")
    public String reserveCard(@PathVariable String roomId, @RequestParam("cardIndex") int cardIndex,
            HttpSession session) {
        String userUuid = (String) session.getAttribute("userUuid");

        if (gamePlayService.reserveCard(roomId, userUuid, cardIndex)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    /**
        * Current player discards one coin while over the limit.
     */
    @PostMapping("/discard-coins")
    public String discardCoins(@PathVariable String roomId, @RequestParam("color") String color, HttpSession session) {
        String userUuid = (String) session.getAttribute("userUuid");

        if (gamePlayService.discardCoins(roomId, userUuid, color)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    /**
     * Pushes a refresh event to everyone in the room.
     */
    private void refreshRoom(String roomId) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
    }
}