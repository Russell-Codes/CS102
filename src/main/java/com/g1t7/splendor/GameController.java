package com.g1t7.splendor;

import com.g1t7.splendor.model.AIPlayer;
import com.g1t7.splendor.model.Card;
import com.g1t7.splendor.model.Game;
import com.g1t7.splendor.model.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * GameController acts as the primary web layer (Controller in MVC architecture)
 * for the active Splendor game session.
 * 
 * It handles HTTP requests during the core gameplay loop, managing the state of 
 * the game via the user's HttpSession. It heavily utilizes the Post-Redirect-Get (PRG) 
 * pattern to prevent duplicate form submissions and ensure safe, uniform state transitions.
 */
@Controller
@RequestMapping("/game/{roomId}")
public class GameController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private GameManager gameManager;

    /**
     * Renders the primary game board view.
     * Validates the session state to ensure an active game exists. If the game has
     * reached a terminal state (game over), it automatically reroutes flow to the gameover view.
     *
     * @param session The active user's HTTP session holding game state.
     * @param model   The Spring MVC model used to inject data into the Thymeleaf template.
     * @return The canonical name of the Thymeleaf HTML template to render, or a redirect directive.
     */
    @GetMapping
    public String showGame(@PathVariable String roomId, Model model, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null || !game.isStarted())
            return "redirect:/";
        if (game.isGameOver())
            return "redirect:/gameover/" + roomId;

        model.addAttribute("game", game);
        model.addAttribute("roomId", roomId);
        model.addAttribute("myUuid", session.getAttribute("userUuid"));
        return "game";
    }

    // --- HEARTBEAT PING ---
    @PostMapping("/ping")
    @ResponseBody
    public String handlePing(@PathVariable String roomId, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        String myUuid = (String) session.getAttribute("userUuid");
        if (game != null && myUuid != null) {
            for (Player p : game.getPlayers()) {
                if (myUuid.equals(p.getUuid())) {
                    p.setLastHeartbeat(System.currentTimeMillis());
                    break;
                }
            }
        }
        return "ok";
    }

    /**
     * Handles the player's action to collect gem coins from the bank.
     * Adheres to Splendor's core rules: drawing 3 distinct coins or 2 of the same coin.
     * Implements strict validation to halt action if the player's coin limit (>10) is exceeded,
     * throwing a pending discard state which must be resolved before the turn progresses.
     * 
     * Uses PRG (Post-Redirect-Get) to safely mutate model state.
     *
     * @param white   Amount of white coins requested.
     * @param blue    Amount of blue coins requested.
     * @param green   Amount of green coins requested.
     * @param red     Amount of red coins requested.
     * @param black   Amount of black coins requested.
     * @param session The HTTP session storing the persistent game object.
     * @return A redirect URI directive to refresh the game board.
     */
    // --- HOST ACTIONS ---
    @PostMapping("/host-action/ai")
    public String replaceWithAi(@PathVariable String roomId, @RequestParam String targetUuid, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game != null && game.getHostUuid().equals(session.getAttribute("userUuid"))) {
            for (Player p : game.getPlayers()) {
                if (p.getUuid() != null && p.getUuid().equals(targetUuid)) {
                    p.setAi(true);
                    p.setName(p.getName() + " (CPU Replaced)");
                    if (game.getCurrentPlayer() == p) {
                        AIPlayer.takeTurn(game, p);
                        game.changeTurns();
                    }
                    messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
                    break;
                }
            }
        }
        return "redirect:/game/" + roomId;
    }

    @PostMapping("/host-action/eject")
    public String ejectPlayer(@PathVariable String roomId, @RequestParam String targetUuid, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game != null && game.getHostUuid().equals(session.getAttribute("userUuid"))) {
            for (Player p : game.getPlayers()) {
                if (p.getUuid() != null && p.getUuid().equals(targetUuid)) {
                    for (int i = 0; i < 6; i++) {
                        game.getBankCoins()[i] += p.getMycoins()[i];
                        p.getMycoins()[i] = 0;
                    }
                    for (Card c : p.getReservedCards()) {
                        c.setReserved(false);
                        if (c.getTier() == 1)
                            game.getTier1Deck().add(0, c);
                        if (c.getTier() == 2)
                            game.getTier2Deck().add(0, c);
                        if (c.getTier() == 3)
                            game.getTier3Deck().add(0, c);
                    }
                    p.getReservedCards().clear();
                    p.setEjected(true);
                    p.setName(p.getName() + " (EJECTED)");

                    if (game.getCurrentPlayer() == p) {
                        game.changeTurns();
                    }
                    messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
                    break;
                }
            }
        }
        return "redirect:/game/" + roomId;
    }

    // --- GAME ACTIONS ---
    @PostMapping("/take-coins")
    public String takeCoins(@PathVariable String roomId, @RequestParam(defaultValue = "0") int white,
            @RequestParam(defaultValue = "0") int blue, @RequestParam(defaultValue = "0") int green,
            @RequestParam(defaultValue = "0") int red, @RequestParam(defaultValue = "0") int black,
            HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/";

        Player current = game.getCurrentPlayer();
        if (!current.getUuid().equals(session.getAttribute("userUuid")))
            return "redirect:/game/" + roomId;

        List<String> selectedColors = new ArrayList<>();
        addColor(selectedColors, "WHITE", white);
        addColor(selectedColors, "BLUE", blue);
        addColor(selectedColors, "GREEN", green);
        addColor(selectedColors, "RED", red);
        addColor(selectedColors, "BLACK", black);

        if (selectedColors.isEmpty() || game.isPendingDiscard())
            return "redirect:/game/" + roomId;

        if (current.exchangeCoin(selectedColors)) {
            if (current.getTotalCoins() > 10)
                game.setPendingDiscard(true);
            else
                game.changeTurns();
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        }
        return "redirect:/game/" + roomId;
    }

    /**
     * Helper utility bound to the takeCoins transaction.
     * Normalizes frontend form data into deterministic list entries. Protects the internal
     * system from malicious payload inputs by clamping values strictly between 0 and 2.
     *
     * @param list  The accumulated list of coin requests.
     * @param color The string representation of the GemColor enum.
     * @param count The raw integer count submitted by the frontend payload.
     */
    private void addColor(List<String> list, String color, int count) {
        for (int i = 0; i < Math.min(Math.max(count, 0), 2); i++)
            list.add(color);
    }

    // -------------------------------------------------------------------------
    // POST /game/buy-card (PRG)    
    // cardIndex ≥ 0 → visible board slot; < 0 → reserved card (-1 = slot 0, etc.)
    // -------------------------------------------------------------------------

    /**
     * Processes the transaction of a player purchasing a development card, either directly
     * from the visible board or from their private reserved hand.
     * Modifies the player's capital and card inventory, recalculates board state, and 
     * evaluates end-of-turn noble visits. Follows the PRG pattern.
     *
     * @param cardIndex Board positional index (>= 0) or offset mapped reserved card index (< 0).
     * @param session   The HTTP session resolving the current game state context.
     * @return A redirect URI directive to clear the POST payload and refresh the game board.
     */
    @PostMapping("/buy-card")
    public String buyCard(@PathVariable String roomId, @RequestParam("cardIndex") int cardIndex, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/";

        Player current = game.getCurrentPlayer();
        if (!current.getUuid().equals(session.getAttribute("userUuid")) || game.isPendingDiscard())
            return "redirect:/game/" + roomId;

        Card card = resolveCard(game, current, cardIndex);
        if (card != null && current.buyCard(card)) {
            if (cardIndex >= 0)
                game.replenishCard(cardIndex);
            else
                current.getReservedCards().remove(card);
            game.changeTurns();
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        }
        return "redirect:/game/" + roomId;
    }

    // -------------------------------------------------------------------------
    // POST /game/reserve-card (PRG)
    // -------------------------------------------------------------------------

    /**
     * Executes the 'reserve card' action, pulling a visible board card into a player's private hand
     * and awarding a wildcard gold token if available in the bank. Enforces the > 10 coin 
     * hand limit rule, triggering a discard phase if necessary. Uses the PRG pattern.
     *
     * @param cardIndex The 1D mapped array index (0-11) of the targeted visible board card.
     * @param session   The HTTP session containing the core Game model.
     * @return A redirect URI directive to refresh the game board rendering.
     */
    @PostMapping("/reserve-card")
    public String reserveCard(@PathVariable String roomId, @RequestParam("cardIndex") int cardIndex,
            HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/";

        Player current = game.getCurrentPlayer();
        if (!current.getUuid().equals(session.getAttribute("userUuid")) || game.isPendingDiscard())
            return "redirect:/game/" + roomId;

        if (cardIndex >= 0 && cardIndex < game.getVisibleCards().size()) {
            Card card = game.getVisibleCards().get(cardIndex);
            if (card != null && current.escortCard(card)) {
                game.replenishCard(cardIndex);
                if (current.getTotalCoins() > 10)
                    game.setPendingDiscard(true);
                else
                    game.changeTurns();
                messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
            }
        }
        return "redirect:/game/" + roomId;
    }

    // -------------------------------------------------------------------------
    // POST /game/discard-coins (PRG)
    // -------------------------------------------------------------------------

    /**
     * Resolves the pending discard blockage state. Invoked when a player's inventory
     * exceeds the strict 10-coin maximum capacity. Discards the target coin back into the 
     * bank. Once exact compliance (<= 10) is achieved, it finalises the suspended turn transition.
     *
     * @param color   The string literal representing the color of the coin to be surrendered.
     * @param session The contextual HTTP session holding the suspended Game state.
     * @return Returns a PRG redirect triggering a board refresh or the next player's turn.
     */
    @PostMapping("/discard-coins")
    public String discardCoins(@PathVariable String roomId, @RequestParam("color") String color, HttpSession session) {
        Game game = gameManager.getGame(roomId);
        if (game == null)
            return "redirect:/";

        Player current = game.getCurrentPlayer();
        if (!current.getUuid().equals(session.getAttribute("userUuid")) || !game.isPendingDiscard())
            return "redirect:/game/" + roomId;

        boolean ok = current.discardCoin(color);
        if (ok && current.getTotalCoins() <= 10) {
            game.setPendingDiscard(false);
            game.changeTurns();
        }
        if (ok)
            messagingTemplate.convertAndSend("/topic/room/" + roomId, "REFRESH");
        return "redirect:/game/" + roomId;
    }

    // -------------------------------------------------------------------------
    // GET /gameover
    // -------------------------------------------------------------------------

    /**
     * Renders the game over terminal state view. Calculates and injects the final 
     * winning player into the model for UI display.
     *
     * @param session The user's HTTP session holding the completed game cache.
     * @param model   The structural model used to inject the 'winner' and 'game' objects into the View.
     * @return The String identifier representing the `gameover` Thymeleaf template.
     */
    @GetMapping("/gameover")
    public String showGameOver(HttpSession session, Model model) {
        Game game = (Game) session.getAttribute("game");
        if (game == null)
            return "redirect:/";
        model.addAttribute("game", game);
        model.addAttribute("winner", game.getWinner());
        return "gameover";
    }

    // -------------------------------------------------------------------------
    // POST /gameover/restart
    // -------------------------------------------------------------------------

    /**
     * Reinitializes the application's runtime state. This is executed typically from the game over screen 
     * or a mid-game abort. It forcefully destroys the HttpSession, effectively garbage collecting 
     * the current Game object and forcing users into a clean login flow.
     *
     * @param session The targeted HTTP Session to be invalidated.
     * @return A redirect mapping directly to the application root (LoginController).
     */
    @PostMapping("/gameover/restart")
    public String restart(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * An analytical helper function resolving 1D integer indices generated by the frontend View 
     * into authoritative `Card` object references in backend memory. 
     *
     * Mathematical mapping:
     * cardIndex >= 0 → Maps directly to the visible board layout array.
     * cardIndex < 0  → Performs an offset recalculation `-(cardIndex + 1)` resolving to the 
     *                  player's private reserved deck. E.g., -1 → 0th reserved, -2 → 1st reserved.
     *
     * @param game      The active game environment.
     * @param player    The active Player entity holding reserved memory bounds.
     * @param cardIndex The raw index passed by request payload.
     * @return The specific Card object reference in memory, or null if strictly out of bounds.
     */
    private Card resolveCard(Game game, Player player, int cardIndex) {
        if (cardIndex >= 0 && cardIndex < game.getVisibleCards().size())
            return game.getVisibleCards().get(cardIndex);
        int reservedIdx = -(cardIndex + 1);
        if (reservedIdx >= 0 && reservedIdx < player.getReservedCards().size())
            return player.getReservedCards().get(reservedIdx);
        return null;
    }
}