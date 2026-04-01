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
 * Handles the game-over page and restart flow.
 */
@Controller
@RequestMapping("/gameover")
public class GameOverController {

    private final GameManager gameManager;

    public GameOverController(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
        * Shows the game-over screen for a room.
     *
        * @param roomId finished room ID
        * @param model page model
        * @return game-over template, or redirect to home if room is missing
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
        * Resets the session and sends the player back to home.
     *
        * @param session current HTTP session
        * @return redirect to home
     */
    @PostMapping("/restart")
    public String restart(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}