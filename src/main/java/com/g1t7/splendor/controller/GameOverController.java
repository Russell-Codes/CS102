package com.g1t7.splendor.controller;

import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.service.GameManager;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller responsible for handling the end-of-game state and routing.
 */
@Controller
@RequestMapping("/gameover")
public class GameOverController {

    private final GameManager gameManager;

    public GameOverController(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Renders the game over terminal state view.
     * Retrieves the game from the GameManager and injects the winner into the
     * model.
     *
     * @param roomId The ID of the room that has finished.
     * @param model  The structural model used to inject the 'winner' and 'game'
     *               objects into the View.
     * @return The String identifier representing the `gameover` Thymeleaf template.
     */
    @GetMapping("/{roomId}")
    public String showGameOver(@PathVariable String roomId, Model model) {
        Game game = gameManager.getGame(roomId);

        if (game == null) {
            return "redirect:/";
        }

        model.addAttribute("game", game);
        model.addAttribute("winner", game.getWinner());
        return "gameover";
    }

    /**
     * Reinitializes the user's session state.
     * Forcefully destroys the HttpSession, forcing the user into a clean login
     * flow.
     * The GameManager will automatically garbage collect the orphaned Game room
     * after it idles out.
     *
     * @param session The targeted HTTP Session to be invalidated.
     * @return A redirect mapping directly to the application root.
     */
    @PostMapping("/restart")
    public String restart(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}