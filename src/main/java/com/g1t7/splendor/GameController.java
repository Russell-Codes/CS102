package com.g1t7.splendor;

import com.g1t7.splendor.model.Card;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Noble;
import com.g1t7.splendor.model.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/game/{roomId}")
public class GameController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private GameManager gameManager;
    @Autowired
    private PlayerActionService playerActionService; // <-- NEW SERVICE INJECTED

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
        if (gameManager.replacePlayerWithAi(roomId, sessionUuid, targetUuid)) {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/host-action/eject")
    public String ejectPlayer(@PathVariable String roomId, @RequestParam String targetUuid, HttpSession session) {
        String sessionUuid = (String) session.getAttribute("userUuid");
        if (gameManager.ejectPlayer(roomId, sessionUuid, targetUuid)) {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/take-coins")
    public String takeCoins(@PathVariable String roomId, @RequestParam(defaultValue = "0") int white,
            @RequestParam(defaultValue = "0") int blue, @RequestParam(defaultValue = "0") int green,
            @RequestParam(defaultValue = "0") int red, @RequestParam(defaultValue = "0") int black,
            HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/";

        Player current = game.getCurrentPlayer();
        if (!current.getUuid().equals(session.getAttribute("userUuid")))
            return "redirect:/game/" + roomId;

        List<String> selectedColors = new ArrayList<>();
        addColor(selectedColors, "WHITE", white);
        addColor(selectedColors, "BLUE", blue);
        addColor(selectedColors, "GREEN", green);
        addColor(selectedColors, "RED", red);
        addColor(selectedColors, "BLACK", black);

        if (selectedColors.isEmpty() || game.isPendingDiscard() || game.isPendingNobleChoice())
            return "redirect:/game/" + roomId;

        // --- UPDATED LOGIC CALL ---
        if (playerActionService.exchangeCoin(game, current, selectedColors)) {
            if (current.getTotalCoins() > Player.MAX_COIN_LIMIT)
                game.setPendingDiscard(true);
            else
                game.changeTurns();
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        }
        return "redirect:/game/" + roomId;
    }

    private void addColor(List<String> list, String color, int count) {
        for (int i = 0; i < Math.min(Math.max(count, 0), 2); i++)
            list.add(color);
    }

    @PostMapping("/buy-card")
    public String buyCard(@PathVariable String roomId, @RequestParam("cardIndex") int cardIndex, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/";

        Player current = game.getCurrentPlayer();
        if (!current.getUuid().equals(session.getAttribute("userUuid")) || game.isPendingDiscard()
                || game.isPendingNobleChoice())
            return "redirect:/game/" + roomId;

        Card card = resolveCard(game, current, cardIndex);

        // --- UPDATED LOGIC CALL ---
        if (card != null && playerActionService.buyCard(game, current, card)) {
            if (cardIndex >= 0)
                game.replenishCard(cardIndex);
            else
                current.getReservedCards().remove(card);

            if (!game.isPendingNobleChoice())
                game.changeTurns();
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/claim-noble")
    public String claimNoble(@PathVariable String roomId, @RequestParam("nobleIndex") int nobleIndex,
            HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/";

        Player current = game.getCurrentPlayer();
        if (!current.getUuid().equals(session.getAttribute("userUuid")) || !game.isPendingNobleChoice()) {
            return "redirect:/game/" + roomId;
        }

        if (nobleIndex >= 0 && nobleIndex < game.getPendingNobles().size()) {
            Noble chosen = game.getPendingNobles().get(nobleIndex);
            current.setScore(current.getScore() + chosen.getVictoryPoints());
            current.getObtainedNobles().add(chosen);

            game.getActiveNobles().remove(chosen);
            game.setPendingNobleChoice(false);
            game.getPendingNobles().clear();

            game.changeTurns();
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/reserve-card")
    public String reserveCard(@PathVariable String roomId, @RequestParam("cardIndex") int cardIndex,
            HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/";

        Player current = game.getCurrentPlayer();
        if (!current.getUuid().equals(session.getAttribute("userUuid")) || game.isPendingDiscard()
                || game.isPendingNobleChoice())
            return "redirect:/game/" + roomId;

        if (cardIndex >= 0 && cardIndex < game.getVisibleCards().size()) {
            Card card = game.getVisibleCards().get(cardIndex);

            // --- UPDATED LOGIC CALL ---
            if (card != null && playerActionService.escortCard(game, current, card)) {
                game.replenishCard(cardIndex);
                if (current.getTotalCoins() > Player.MAX_COIN_LIMIT)
                    game.setPendingDiscard(true);
                else
                    game.changeTurns();
                messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
            }
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/discard-coins")
    public String discardCoins(@PathVariable String roomId, @RequestParam("color") String color, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/";

        Player current = game.getCurrentPlayer();
        if (!current.getUuid().equals(session.getAttribute("userUuid")) || !game.isPendingDiscard()
                || game.isPendingNobleChoice())
            return "redirect:/game/" + roomId;

        // --- UPDATED LOGIC CALL ---
        boolean ok = playerActionService.discardCoin(game, current, color);

        if (ok && current.getTotalCoins() <= Player.MAX_COIN_LIMIT) {
            game.setPendingDiscard(false);
            game.changeTurns();
        }
        if (ok)
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        return "redirect:/game/" + roomId;
    }

    private Card resolveCard(Game game, Player player, int cardIndex) {
        if (cardIndex >= 0 && cardIndex < game.getVisibleCards().size())
            return game.getVisibleCards().get(cardIndex);
        int reservedIdx = -(cardIndex + 1);
        if (reservedIdx >= 0 && reservedIdx < player.getReservedCards().size())
            return player.getReservedCards().get(reservedIdx);
        return null;
    }
}