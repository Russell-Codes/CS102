package com.g1t7.splendor.controller;

import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.service.GameManager;
import com.g1t7.splendor.service.LobbyService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Handles entry flow into the application: landing page, room creation, and room join.
 */
@Controller
public class LoginController {

    private GameManager gameManager;
    private LobbyService lobbyService;

    public LoginController(GameManager gameManager, LobbyService lobbyService) {
        this.gameManager = gameManager;
        this.lobbyService = lobbyService;
    }

    /**
     * Ensures the current session has a stable player UUID.
     */
    private String ensureUuid(HttpSession session) {
        String uuid = (String) session.getAttribute("userUuid");
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            session.setAttribute("userUuid", uuid);
        }
        return uuid;
    }

    /**
     * Renders the login/landing page and initializes session UUID if needed.
     */
    @GetMapping("/")
    public String showLogin(HttpSession session) {
        ensureUuid(session);
        return "login";
    }

    /**
     * Creates a new lobby with the current user as host.
     */
    @PostMapping("/start")
    public String createLobby(@RequestParam(defaultValue = "2") int numPlayers,
            @RequestParam String hostName,
            HttpSession session) {
        String hostUuid = ensureUuid(session);
        String roomId = lobbyService.createNewRoom(numPlayers, hostName, hostUuid);

        if (roomId == null) {
            return "redirect:/?error=serverfull";
        }

        return "redirect:/lobby/" + roomId;
    }

    /**
     * Joins an existing room when room/state checks allow it.
     */
    @GetMapping("/join")
    public String joinLobby(@RequestParam String roomId, HttpSession session) {
        String myUuid = ensureUuid(session);
        Game game = gameManager.getGame(roomId);

        if (game == null)
            return "redirect:/?error=notfound";

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