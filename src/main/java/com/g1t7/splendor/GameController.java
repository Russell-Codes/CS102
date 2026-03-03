package com.g1t7.splendor;

import com.g1t7.splendor.model.Card;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class GameController {

    // -------------------------------------------------------------------------
    // GET /game
    // -------------------------------------------------------------------------

    @GetMapping("/game")
    public String showGame(HttpSession session, Model model) {
        Game game = (Game) session.getAttribute("game");
        if (game == null) return "redirect:/";
        if (game.isGameOver()) return "redirect:/gameover";
        model.addAttribute("game", game);
        return "game";
    }

    // -------------------------------------------------------------------------
    // POST /game/take-coins   (PRG)
    // -------------------------------------------------------------------------

    @PostMapping("/game/take-coins")
    public String takeCoins(
            @RequestParam(defaultValue = "0") int white,
            @RequestParam(defaultValue = "0") int blue,
            @RequestParam(defaultValue = "0") int green,
            @RequestParam(defaultValue = "0") int red,
            @RequestParam(defaultValue = "0") int black,
            HttpSession session) {

        Game game = (Game) session.getAttribute("game");
        if (game == null) return "redirect:/";

        // Build color list by repeating each color the requested number of times (clamped 0-2)
        List<String> selectedColors = new ArrayList<>();
        addColor(selectedColors, "WHITE", white);
        addColor(selectedColors, "BLUE",  blue);
        addColor(selectedColors, "GREEN", green);
        addColor(selectedColors, "RED",   red);
        addColor(selectedColors, "BLACK", black);

        if (selectedColors.isEmpty()) {
            game.setMessage("Please select at least 1 coin.");
            return "redirect:/game";
        }

        boolean ok = game.getCurrentPlayer().exchangeCoin(selectedColors);
        if (ok) game.changeTurns();
        return "redirect:/game";
    }

    private void addColor(List<String> list, String color, int count) {
        for (int i = 0; i < Math.min(Math.max(count, 0), 2); i++) list.add(color);
    }

    // -------------------------------------------------------------------------
    // POST /game/buy-card   (PRG)
    // cardIndex ≥ 0 → visible board slot; < 0 → reserved card (-1 = slot 0, etc.)
    // -------------------------------------------------------------------------

    @PostMapping("/game/buy-card")
    public String buyCard(
            @RequestParam("cardIndex") int cardIndex,
            HttpSession session) {

        Game game = (Game) session.getAttribute("game");
        if (game == null) return "redirect:/";

        Player current = game.getCurrentPlayer();
        Card card = resolveCard(game, current, cardIndex);
        if (card == null) {
            game.setMessage("Invalid card selection.");
            return "redirect:/game";
        }

        boolean ok = current.buyCard(card);
        if (ok) {
            if (cardIndex >= 0) {
                game.replenishCard(cardIndex);
            } else {
                // Remove from player's reserved list (already done in buyCard via card reference)
                current.getReservedCards().remove(card);
            }
            game.changeTurns();
        }
        return "redirect:/game";
    }

    // -------------------------------------------------------------------------
    // POST /game/reserve-card   (PRG)
    // -------------------------------------------------------------------------

    @PostMapping("/game/reserve-card")
    public String reserveCard(
            @RequestParam("cardIndex") int cardIndex,
            HttpSession session) {

        Game game = (Game) session.getAttribute("game");
        if (game == null) return "redirect:/";

        Player current = game.getCurrentPlayer();

        // Only visible board cards can be reserved (cardIndex 0–11)
        if (cardIndex < 0 || cardIndex >= game.getVisibleCards().size()) {
            game.setMessage("Cannot reserve that card.");
            return "redirect:/game";
        }

        Card card = game.getVisibleCards().get(cardIndex);
        if (card == null) {
            game.setMessage("No card in that slot.");
            return "redirect:/game";
        }

        boolean ok = current.escortCard(card);
        if (ok) {
            game.replenishCard(cardIndex);
            game.changeTurns();
        }
        return "redirect:/game";
    }

    // -------------------------------------------------------------------------
    // GET /gameover
    // -------------------------------------------------------------------------

    @GetMapping("/gameover")
    public String showGameOver(HttpSession session, Model model) {
        Game game = (Game) session.getAttribute("game");
        if (game == null) return "redirect:/";
        model.addAttribute("game", game);
        model.addAttribute("winner", game.getWinner());
        return "gameover";
    }

    // -------------------------------------------------------------------------
    // POST /gameover/restart
    // -------------------------------------------------------------------------

    @PostMapping("/gameover/restart")
    public String restart(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * cardIndex >= 0 → visible board slot.
     * cardIndex < 0  → reserved card: decode as -(reservedIndex + 1).
     *   e.g. -1 → reservedCards.get(0), -2 → get(1), -3 → get(2)
     */
    private Card resolveCard(Game game, Player player, int cardIndex) {
        if (cardIndex >= 0) {
            List<Card> visible = game.getVisibleCards();
            if (cardIndex >= visible.size()) return null;
            return visible.get(cardIndex);
        } else {
            int reservedIdx = -(cardIndex + 1);
            List<Card> reserved = player.getReservedCards();
            if (reservedIdx >= reserved.size()) return null;
            return reserved.get(reservedIdx);
        }
    }
}
