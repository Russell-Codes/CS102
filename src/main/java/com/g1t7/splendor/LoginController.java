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

/**
 * LoginController resolves the entry point of the Splendor Web Application.
 * As a Spring MVC Controller, it handles initial routing before a game session begins,
 * parsing player configurations and seeding the primary HTTP Session with game state.
 */
@Controller
public class LoginController {

    /**
     * Resolves the root URI mapping ('/') and delivers the initial login interface.
     * Injects a clean Login Data Transfer Object (DTO) into the model to capture user inputs.
     *
     * @param model The Spring UI Model used to bind the empty Login object.
     * @return The thymeleaf template name 'login'.
     */
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

    /**
     * Bootstraps the core Game model based on the captured login configuration setup.
     * Ensures strict mathematical bounds on the number of players (2 to 4), dynamically 
     * resolves human versus AI assignments, initializes the board, and attaches the entire 
     * constructed context to the user's volatile HttpSession.
     *
     * Automatically triggers the core turn processing loop if Player 1 is resolved as an AI.
     *
     * @param login   The fully hydrated backend mapping of the frontend HTML form.
     * @param session The contextual HTTP session initialized for the connecting browser.
     * @return A PRG redirect string dispatching the client to the active game board mapping.
     */
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