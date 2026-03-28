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
    public String createLobby(@RequestParam int numPlayers,
            @RequestParam String hostName,
            HttpSession session) {
        String hostUuid = ensureUuid(session);

        Game game = new Game();
        game.setCapacity(Math.max(2, Math.min(4, numPlayers)));
        game.setStarted(false);
        game.setHostUuid(hostUuid);

        // NEW: Automatically add the host as the first player in the lobby
        Player host = new Player(game, hostName.trim());
        host.setUuid(hostUuid);
        host.setReady(true); // Host is ready by default since they just set up the game
        game.getPlayers().add(host);

        String roomId = gameManager.createGame(game);
        return "redirect:/lobby/" + roomId;
    }

    @GetMapping("/join")
    public String joinLobby(@RequestParam String roomId, HttpSession session) {
        ensureUuid(session);
        return "redirect:/lobby/" + roomId.toUpperCase();
    }
}