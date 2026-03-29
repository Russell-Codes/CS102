package com.g1t7.splendor;

import com.g1t7.splendor.model.AIPlayer;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Login;
import com.g1t7.splendor.model.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Set;

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
    @GetMapping("/")
    public String showLogin(Model model) {
        model.addAttribute("login", new Login());
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
    public String startGame(@ModelAttribute Login login, HttpSession session) {
        int numPlayers = Math.max(2, Math.min(4, login.getNumPlayers()));
        Set<String> aiSet = login.getAiPlayers() != null
                ? Set.copyOf(login.getAiPlayers())
                : Set.of();

        String[] rawNames = {
            login.getPlay1(),
            login.getPlay2(),
            login.getPlay3(),
            login.getPlay4()
        };

        Game game = new Game();

        // Scans the sanitized player 
        // configuration array to provision new structural Player model instances. Applies default 
        // names if skipped, identifies AI actors via the Set lookup, and appends a visual robot flag.
        for (int i = 0; i < numPlayers; i++) {
            String defaultName = "Player " + (i + 1);
            String name = (rawNames[i] == null || rawNames[i].isBlank()) ? defaultName : rawNames[i].trim();
            
            boolean isAi = aiSet.contains("ai" + (i + 1));
            if (isAi) name = name + " \uD83E\uDD16"; // robot emoji suffix for AI visually distinguishing bots in the View layer
            Player p = new Player(game, name, isAi);
            game.getPlayers().add(p);
        }

        game.variableInit();
        session.setAttribute("game", game);

        // If the first player is AI, make them move immediately
        if (game.getCurrentPlayer().isAi()) {
            AIPlayer.takeTurn(game, game.getCurrentPlayer());
            game.changeTurns();
        }

        return "redirect:/game";
    }
}