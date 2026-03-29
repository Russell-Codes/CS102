package com.g1t7.splendor;

import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class LoginController {

    @Autowired
    private GameManager gameManager;

    // Helper to ensure every visitor has a unique ID
    private String ensureUuid(HttpSession session) {
        String uuid = (String) session.getAttribute("userUuid");
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            session.setAttribute("userUuid", uuid);
        }
        return uuid;
    }

    @GetMapping("/")
    public String showLogin(HttpSession session) {
        ensureUuid(session);
        return "login";
    }

    @PostMapping("/start")
    public String createLobby(@RequestParam(defaultValue = "2") int numPlayers,
            @RequestParam String hostName,
            HttpSession session) {
        String hostUuid = ensureUuid(session);

        Game game = new Game();
        game.setCapacity(Math.max(2, Math.min(4, numPlayers)));
        game.setStarted(false);
        game.setHostUuid(hostUuid);

        // Host is automatically added as the first player and readied up
        Player host = new Player(game, hostName.trim());
        host.setUuid(hostUuid);
        host.setReady(true);
        game.getPlayers().add(host);

        String roomId = gameManager.createGame(game);
        return "redirect:/lobby/" + roomId;
    }

    @GetMapping("/join")
    public String joinLobby(@RequestParam String roomId, HttpSession session) {
        String myUuid = ensureUuid(session);
        Game game = gameManager.getGame(roomId);

        // BOUNCER CHECKS
        if (game == null) {
            return "redirect:/?error=notfound";
        }

        boolean alreadyInRoom = game.getPlayers().stream()
                .anyMatch(p -> p.getUuid() != null && p.getUuid().equals(myUuid));

        if (!alreadyInRoom && game.getPlayers().size() >= game.getCapacity()) {
            return "redirect:/?error=full";
        }

        if (!alreadyInRoom && game.isStarted()) {
            return "redirect:/?error=started";
        }

        return "redirect:/lobby/" + roomId.toUpperCase();
    }
}