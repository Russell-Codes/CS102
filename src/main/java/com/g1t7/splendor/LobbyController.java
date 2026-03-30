package com.g1t7.splendor;

import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/lobby/{roomId}")
public class LobbyController {

    @Autowired
    private GameManager gameManager;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public String showLobby(@PathVariable String roomId, Model model, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/?error=notfound";

        String myUuid = (String) session.getAttribute("userUuid");
        Player me = game.getPlayers().stream()
                .filter(p -> p.getUuid() != null && p.getUuid().equals(myUuid))
                .findFirst().orElse(null);

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
        Game game = gameManager.getGame(roomId);
        if (game == null || game.isStarted())
            return "redirect:/";

        String myUuid = (String) session.getAttribute("userUuid");
        Player me = game.getPlayers().stream().filter(p -> p.getUuid().equals(myUuid)).findFirst().orElse(null);

        if (me == null) {
            if (game.getPlayers().size() < game.getCapacity()) {
                me = new Player(game, playerName.trim());
                me.setUuid(myUuid);
                game.getPlayers().add(me);
            } else {
                return "redirect:/lobby/" + roomId + "?error=full";
            }
        } else {
            me.setName(playerName.trim());
        }

        me.setReady(true);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        return "redirect:/lobby/" + roomId;
    }

    @PostMapping("/add-ai")
    public String addAi(@PathVariable String roomId, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        String myUuid = (String) session.getAttribute("userUuid");

        if (game != null && game.getHostUuid().equals(myUuid) && game.getPlayers().size() < game.getCapacity()) {
            int aiCount = (int) game.getPlayers().stream().filter(Player::isAi).count() + 1;
            Player aiPlayer = new Player(game, "CPU " + aiCount, true);
            aiPlayer.setUuid(UUID.randomUUID().toString()); // So we can target it for removal
            aiPlayer.setReady(true);

            game.getPlayers().add(aiPlayer);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        }
        return "redirect:/lobby/" + roomId;
    }

    @PostMapping("/remove-ai")
    public String removeAi(@PathVariable String roomId, @RequestParam String targetUuid, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        String myUuid = (String) session.getAttribute("userUuid");

        if (game != null && game.getHostUuid().equals(myUuid)) {
            game.getPlayers().removeIf(p -> p.isAi() && p.getUuid().equals(targetUuid));
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        }
        return "redirect:/lobby/" + roomId;
    }

    @PostMapping("/start")
    public String startGame(@PathVariable String roomId, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        String myUuid = (String) session.getAttribute("userUuid");

        if (game != null && game.getHostUuid().equals(myUuid) && game.getPlayers().size() == game.getCapacity()) {
            boolean allReady = game.getPlayers().stream().allMatch(Player::isReady);
            if (allReady) {
                game.setStarted(true);
                game.variableInit();
                messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
            }
        }
        return "redirect:/game/" + roomId;
    }
}