package com.g1t7.splendor;

import com.g1t7.splendor.model.Game;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/gameover")
public class GameOverController {

    @Autowired
    private GameManager gameManager;

    // -------------------------------------------------------------------------
    // GET /gameover
    // -------------------------------------------------------------------------

    /**
     * Renders the game over terminal state view. Calculates and injects the final
     * winning player into the model for UI display.
     *
     * @param session The user's HTTP session holding the completed game cache.
     * @param model   The structural model used to inject the 'winner' and 'game'
     *                objects into the View.
     * @return The String identifier representing the `gameover` Thymeleaf template.
     */
    @GetMapping("/{roomId}")
    public String showGameOver(@PathVariable String roomId, Model model) {
        // Fetch the game using the GameManager
        Game game = gameManager.getGame(roomId);

        if (game == null) {
            return "redirect:/";
        }

        model.addAttribute("game", game);
        model.addAttribute("winner", game.getWinner());
        return "gameover";
    }

    // -------------------------------------------------------------------------
    // POST /gameover/restart
    // -------------------------------------------------------------------------

    /**
     * Reinitializes the application's runtime state. This is executed typically
     * from the game over screen
     * or a mid-game abort. It forcefully destroys the HttpSession, effectively
     * garbage collecting
     * the current Game object and forcing users into a clean login flow.
     *
     * @param session The targeted HTTP Session to be invalidated.
     * @return A redirect mapping directly to the application root
     *         (LoginController).
     */
    @PostMapping("/restart")
    public String restart(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}