package com.g1t7.splendor.controller;

import com.g1t7.splendor.service.GameManager;
import com.g1t7.splendor.service.GameEngineService;
import com.g1t7.splendor.service.GamePlayService;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/game/{roomId}")
public class GameController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private GameManager gameManager;
    @Autowired
    private GamePlayService gamePlayService;
    @Autowired
    private GameEngineService gameEngineService;

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

    @PostMapping("/host-action/ai")
    public String replaceWithAi(@PathVariable String roomId, @RequestParam String targetUuid, HttpSession session) {
        String sessionUuid = (String) session.getAttribute("userUuid");
        Game game = gameManager.getGame(roomId);

        if (game != null && gameEngineService.replacePlayerWithAi(game, sessionUuid, targetUuid)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/host-action/eject")
    public String ejectPlayer(@PathVariable String roomId, @RequestParam String targetUuid, HttpSession session) {
        String sessionUuid = (String) session.getAttribute("userUuid");
        Game game = gameManager.getGame(roomId);

        if (game != null && gameEngineService.ejectPlayer(game, sessionUuid, targetUuid)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

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

    @PostMapping("/buy-card")
    public String buyCard(@PathVariable String roomId, @RequestParam("cardIndex") int cardIndex, HttpSession session) {
        String userUuid = (String) session.getAttribute("userUuid");

        if (gamePlayService.buyCard(roomId, userUuid, cardIndex)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/claim-noble")
    public String claimNoble(@PathVariable String roomId, @RequestParam("nobleIndex") int nobleIndex,
            HttpSession session) {
        String userUuid = (String) session.getAttribute("userUuid");

        if (gamePlayService.claimNoble(roomId, userUuid, nobleIndex)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/reserve-card")
    public String reserveCard(@PathVariable String roomId, @RequestParam("cardIndex") int cardIndex,
            HttpSession session) {
        String userUuid = (String) session.getAttribute("userUuid");

        if (gamePlayService.reserveCard(roomId, userUuid, cardIndex)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/discard-coins")
    public String discardCoins(@PathVariable String roomId, @RequestParam("color") String color, HttpSession session) {
        String userUuid = (String) session.getAttribute("userUuid");

        if (gamePlayService.discardCoins(roomId, userUuid, color)) {
            refreshRoom(roomId);
        }
        return "redirect:/game/" + roomId;
    }

    private void refreshRoom(String roomId) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
    }
}