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

import java.util.List;
import java.util.Set;

@Controller
public class LoginController {

    @GetMapping("/")
    public String showLogin(Model model) {
        model.addAttribute("login", new Login());
        return "login";
    }

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

        for (int i = 0; i < numPlayers; i++) {
            String defaultName = "Player " + (i + 1);
            String name = (rawNames[i] == null || rawNames[i].isBlank()) ? defaultName : rawNames[i].trim();
            boolean isAi = aiSet.contains("ai" + (i + 1));
            if (isAi) name = name + " \uD83E\uDD16"; // robot emoji suffix for AI
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