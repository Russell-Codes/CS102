package com.g1t7.splendor;

import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Login;
import com.g1t7.splendor.model.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    @GetMapping("/")
    public String showLogin(Model model) {
        model.addAttribute("login", new Login());
        return "login";
    }

    @PostMapping("/start")
    public String startGame(@ModelAttribute Login login, HttpSession session) {
        String name1 = (login.getPlay1() == null || login.getPlay1().isBlank()) ? "Player 1" : login.getPlay1().trim();
        String name2 = (login.getPlay2() == null || login.getPlay2().isBlank()) ? "Player 2" : login.getPlay2().trim();

        Game game = new Game();
        game.variableInit();

        Player p1 = new Player(game, name1);
        Player p2 = new Player(game, name2);
        game.getPlayers()[0] = p1;
        game.getPlayers()[1] = p2;

        session.setAttribute("game", game);
        return "redirect:/game";
    }
}
