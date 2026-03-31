package com.g1t7.splendor;

import com.g1t7.splendor.config.GameConfig;
import com.g1t7.splendor.model.*;
import com.g1t7.splendor.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Splendor game logic – covers coin-taking rules,
 * card buying, reserving, noble visits, game-over / tiebreaker,
 * config loading, CSV card loading, and AI player behaviour.
 */
class GameLogicTests {

    private Game game;
    private Player p1, p2;

    // Services now handle the logic instead of the models
    private PlayerActionService playerActionService;
    private GameEngineService gameEngineService;
    private AIPlayer aiPlayerService;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize our clean architecture services manually for testing
        playerActionService = new PlayerActionService();
        gameEngineService = new GameEngineService();
        aiPlayerService = new AIPlayer(playerActionService, gameEngineService);

        // Inject the AIPlayer service into GameEngineService (resolving the Spring
        // @Autowired)
        Field aiField = GameEngineService.class.getDeclaredField("aiPlayer");
        aiField.setAccessible(true);
        aiField.set(gameEngineService, aiPlayerService);

        game = new Game();

        // FIXED: Player constructor no longer takes the 'game' object
        p1 = new Player("P1");
        p2 = new Player("P2");
        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        gameEngineService.initializeGame(game);
    }

    // ===================== Config =====================

    @Test
    void configLoadsWinScore() {
        GameConfig cfg = new GameConfig();
        assertEquals(15, cfg.getWinScore(), "Default win score should be 15");
    }

    @Test
    void configLoadsGemCounts() {
        GameConfig cfg = new GameConfig();
        assertEquals(4, cfg.getGemCount(2));
        assertEquals(5, cfg.getGemCount(3));
        assertEquals(7, cfg.getGemCount(4));
    }

    @Test
    void configLoadsNobleCountByPlayers() {
        GameConfig cfg = new GameConfig();
        assertEquals(3, cfg.getNobleCount(2));
        assertEquals(4, cfg.getNobleCount(3));
        assertEquals(5, cfg.getNobleCount(4));
    }

    // ===================== Initial State =====================

    @Test
    void initialBankHasCorrectCoins() {
        int[] bank = game.getBankCoins();
        for (int i = 0; i < 5; i++) {
            assertEquals(4, bank[i], "2-player game should have 4 of each gem");
        }
        assertEquals(5, bank[5], "Should have 5 gold coins");
    }

    @Test
    void initialBoardHas12VisibleCards() {
        long filled = game.getVisibleCards().stream().filter(c -> c != null).count();
        assertEquals(12, filled, "Should deal 12 visible cards (4 per tier)");
    }

    @Test
    void initialNoblesCorrectFor2Players() {
        assertEquals(3, game.getActiveNobles().size(), "2-player game should have 3 nobles");
    }

    @Test
    void csvLoads90Cards() {
        List<Card> cards = CardData.buildDeck("game/config/cards.csv");
        assertEquals(90, cards.size(), "CSV should contain 90 cards");
    }

    // ===================== Take Coins =====================

    @Test
    void take3DifferentCoins() {
        boolean ok = playerActionService.exchangeCoin(game, p1, List.of("WHITE", "BLUE", "GREEN"));
        assertTrue(ok, "Taking 3 different should succeed");

        // FIXED: getMyCoins() -> getCoins()
        assertEquals(1, p1.getCoins()[0]); // white
        assertEquals(1, p1.getCoins()[1]); // blue
        assertEquals(1, p1.getCoins()[2]); // green
        assertEquals(3, game.getBankCoins()[0]);
        assertEquals(3, game.getBankCoins()[1]);
        assertEquals(3, game.getBankCoins()[2]);
    }

    @Test
    void take2SameColor() {
        boolean ok = playerActionService.exchangeCoin(game, p1, List.of("RED", "RED"));
        assertTrue(ok, "Taking 2 red should succeed (bank has 4)");
        assertEquals(2, p1.getCoins()[3]);
        assertEquals(2, game.getBankCoins()[3]);
    }

    @Test
    void take2SameFailsWhenBankUnder4() {
        game.getBankCoins()[3] = 3; // only 3 red
        boolean ok = playerActionService.exchangeCoin(game, p1, List.of("RED", "RED"));
        assertFalse(ok, "Should fail: bank < 4 of that color");
    }

    @Test
    void take2DifferentColorsRejected() {
        boolean ok = playerActionService.exchangeCoin(game, p1, List.of("RED", "BLACK"));
        assertFalse(ok, "Taking 2 different colors should be rejected");
    }

    @Test
    void cannotTakeGoldDirectly() {
        boolean ok = playerActionService.exchangeCoin(game, p1, List.of("GOLD"));
        assertFalse(ok, "Cannot take gold coins directly");
    }

    @Test
    void canTakeCoinsOver10ThenDiscard() {
        // Give player 9 coins already
        p1.getCoins()[0] = 3; // WHITE
        p1.getCoins()[1] = 3; // BLUE
        p1.getCoins()[2] = 3; // GREEN

        // Taking 3 more (total = 12) should succeed
        boolean ok = playerActionService.exchangeCoin(game, p1, List.of("RED", "GREEN", "BLACK"));
        assertTrue(ok, "Should allow taking coins over 10");
        assertEquals(12, p1.getTotalCoins());

        // Discard 2 coins to get back to 10
        assertTrue(playerActionService.discardCoin(game, p1, "RED"));
        assertEquals(11, p1.getTotalCoins());
        assertTrue(playerActionService.discardCoin(game, p1, "BLACK"));
        assertEquals(10, p1.getTotalCoins());

        // Cannot discard a color you don't have
        assertFalse(playerActionService.discardCoin(game, p1, "RED"));
    }

    @Test
    void cannotTakeFromEmptyBank() {
        game.getBankCoins()[0] = 0; // white empty
        boolean ok = playerActionService.exchangeCoin(game, p1, List.of("WHITE", "BLUE", "GREEN"));
        assertFalse(ok, "Should fail if selected color has 0 in bank");
    }

    @Test
    void take4CoinsRejected() {
        boolean ok = playerActionService.exchangeCoin(game, p1, List.of("WHITE", "BLUE", "GREEN", "RED"));
        assertFalse(ok, "Cannot take more than 3 coins");
    }

    // ===================== Buy Card =====================

    @Test
    void buyCardDeductsCoinsAndAddsBonus() {
        Card card = new Card(1, GemColor.WHITE, 1, new int[] { 1, 1, 1, 0, 0, 0 });
        p1.getCoins()[0] = 2; // white
        p1.getCoins()[1] = 2; // blue
        p1.getCoins()[2] = 2; // green

        boolean ok = playerActionService.buyCard(game, p1, card);
        assertTrue(ok);
        assertEquals(1, p1.getScore(), "Should gain 1 VP");

        // FIXED: getMyCards() -> getBonuses()
        assertEquals(1, p1.getBonuses()[0], "Should gain white bonus");
        assertEquals(1, p1.getCoins()[0], "Should have 1 white left");
        assertEquals(1, p1.getCoins()[1], "Should have 1 blue left");
        assertEquals(1, p1.getCoins()[2], "Should have 1 green left");
    }

    @Test
    void buyCardUsesGoldForShortfall() {
        Card card = new Card(1, GemColor.BLUE, 0, new int[] { 2, 0, 0, 0, 0, 0 });
        p1.getCoins()[0] = 1; // 1 white, need 2
        p1.getCoins()[5] = 2; // 2 gold

        boolean ok = playerActionService.buyCard(game, p1, card);
        assertTrue(ok, "Should use 1 gold to cover shortfall");
        assertEquals(0, p1.getCoins()[0], "All white spent");
        assertEquals(1, p1.getCoins()[5], "1 gold spent, 1 remaining");
    }

    @Test
    void buyCardAppliesBonusDiscount() {
        Card card = new Card(2, GemColor.GREEN, 2, new int[] { 3, 0, 0, 0, 0, 0 });
        p1.getBonuses()[0] = 2; // 2 white bonuses → effective cost = 1 white
        p1.getCoins()[0] = 1;

        boolean ok = playerActionService.buyCard(game, p1, card);
        assertTrue(ok, "Bonus should reduce cost from 3 to 1");
        assertEquals(2, p1.getScore());
    }

    @Test
    void buyCardFailsIfCannotAfford() {
        Card card = new Card(1, GemColor.RED, 0, new int[] { 5, 0, 0, 0, 0, 0 });
        p1.getCoins()[0] = 1; // only 1 white, need 5
        boolean ok = playerActionService.buyCard(game, p1, card);
        assertFalse(ok, "Should not afford this card");
    }

    @Test
    void buyCardReturnsCoinsToBank() {
        Card card = new Card(1, GemColor.WHITE, 0, new int[] { 2, 0, 0, 0, 0, 0 });
        p1.getCoins()[0] = 3;
        int bankWhiteBefore = game.getBankCoins()[0];

        playerActionService.buyCard(game, p1, card);
        assertEquals(bankWhiteBefore + 2, game.getBankCoins()[0], "Coins should return to bank");
    }

    // ===================== Reserve Card =====================

    @Test
    void reserveCardGivesGoldCoin() {
        Card card = game.getVisibleCards().get(0);
        assertNotNull(card);
        int goldBefore = game.getBankCoins()[5];

        boolean ok = playerActionService.reserveCard(game, p1, card);
        assertTrue(ok);
        assertEquals(1, p1.getReservedCards().size());
        assertEquals(1, p1.getCoins()[5], "Should receive 1 gold");
        assertEquals(goldBefore - 1, game.getBankCoins()[5]);
        assertTrue(card.isReserved());
    }

    @Test
    void cannotReserveMoreThan3Cards() {
        for (int i = 0; i < 3; i++) {
            Card c = new Card(1, GemColor.WHITE, 0, new int[] { 0, 0, 0, 0, 0, 0 });
            playerActionService.reserveCard(game, p1, c);
        }
        Card fourth = new Card(1, GemColor.BLUE, 0, new int[] { 0, 0, 0, 0, 0, 0 });
        boolean ok = playerActionService.reserveCard(game, p1, fourth);
        assertFalse(ok, "Should not allow more than 3 reserved cards");
    }

    @Test
    void reserveGivesNoGoldIfBankEmpty() {
        game.getBankCoins()[5] = 0; // no gold
        Card card = game.getVisibleCards().get(0);
        playerActionService.reserveCard(game, p1, card);
        assertEquals(0, p1.getCoins()[5], "No gold if bank is empty");
    }

    // ===================== Noble Visit =====================

    @Test
    void nobleVisitsWhenRequirementMet() {
        Noble noble = new Noble(3, 0, 0, 0, 0); // requires 3 white cards
        game.setActiveNobles(new java.util.ArrayList<>(List.of(noble)));

        p1.getBonuses()[0] = 3; // 3 white bonuses
        playerActionService.checkNobles(game, p1, game.getActiveNobles());

        assertEquals(3, p1.getScore(), "Should gain 3 VP from noble");
        assertTrue(game.getActiveNobles().isEmpty(), "Noble should be removed");
    }

    @Test
    void nobleDoesNotVisitIfRequirementNotMet() {
        Noble noble = new Noble(4, 0, 0, 0, 0); // requires 4 white
        game.setActiveNobles(new java.util.ArrayList<>(List.of(noble)));

        p1.getBonuses()[0] = 2;
        playerActionService.checkNobles(game, p1, game.getActiveNobles());

        assertEquals(0, p1.getScore());
        assertEquals(1, game.getActiveNobles().size(), "Noble should remain");
    }

    @Test
    void onlyOneNoblePerTurn() {
        Noble n1 = new Noble(1, 0, 0, 0, 0);
        Noble n2 = new Noble(0, 1, 0, 0, 0);
        game.setActiveNobles(new java.util.ArrayList<>(List.of(n1, n2)));

        p1.getBonuses()[0] = 3;
        p1.getBonuses()[1] = 3;
        playerActionService.checkNobles(game, p1, game.getActiveNobles());

        assertEquals(3, p1.getScore(), "Should gain only 3 VP (one noble)");
        assertEquals(1, game.getActiveNobles().size(), "Only one noble removed per turn");
    }

    // ===================== Game Over & Tiebreak =====================

    @Test
    void gameOverAfterFullRound() {
        // P1 reaches winScore
        p1.setScore(15);
        assertFalse(game.isGameOver(), "Game should NOT be over before round ends");

        // Simulate changeTurns from P1's perspective
        game.setCurrentTurnIndex(0);
        gameEngineService.changeTurns(game); // now P2's turn, finalRound triggered
        assertFalse(game.isGameOver(), "P2 should get a turn before game ends");

        // P2 takes their turn
        gameEngineService.changeTurns(game); // back to P1 (starting player), game should end
        assertTrue(game.isGameOver(), "Game should end after full round");
    }

    @Test
    void winnerIsHighestScore() {
        p1.setScore(16);
        p2.setScore(15);
        assertEquals(p1, game.getWinner());
    }

    @Test
    void tiebreakerFewestCards() {
        p1.setScore(15);
        p2.setScore(15);
        // P1 bought 5 cards, P2 bought 3 → P2 wins tiebreaker (fewer cards)
        for (int i = 0; i < 5; i++)
            p1.getCards().push(new Card(1, GemColor.WHITE, 3, new int[6]));
        for (int i = 0; i < 3; i++)
            p2.getCards().push(new Card(1, GemColor.WHITE, 5, new int[6]));

        assertEquals(p2, game.getWinner(), "With same score, fewer cards wins");
    }

    @Test
    void winScoreFromConfig() {
        assertEquals(15, game.getWinScore(), "Win score should match config (15)");
    }

    // ===================== AI Player =====================

    @Test
    void aiPlayerTakesTurn() {
        Player aiPlayer = new Player("AI", true);
        game.getPlayers().add(aiPlayer);
        assertTrue(aiPlayer.isAi());

        boolean acted = aiPlayerService.takeTurn(game, aiPlayer);
        assertTrue(acted, "AI should take a valid action");
        // AI should have either taken coins, bought a card, or reserved one
        boolean hasCoins = aiPlayer.getTotalCoins() > 0;
        boolean hasCards = !aiPlayer.getCards().isEmpty();
        boolean hasReserved = !aiPlayer.getReservedCards().isEmpty();
        assertTrue(hasCoins || hasCards || hasReserved,
                "AI should have gained something (coins, card, or reserve)");
    }

    // ===================== Card Replenishment =====================

    @Test
    void replenishCardFillsEmptySlot() {
        int slot = 0;
        Card original = game.getVisibleCards().get(slot);
        assertNotNull(original);
        int deckSizeBefore = game.getTier1Deck().size();

        gameEngineService.replenishCard(game, slot);
        Card replacement = game.getVisibleCards().get(slot);

        if (deckSizeBefore > 0) {
            assertNotNull(replacement, "Slot should be filled from deck");
            assertNotEquals(original, replacement, "Should be a new card");
        }
    }

    // ===================== Multi-Player Support =====================

    @Test
    void threePlayerGameHasCorrectSetup() {
        Game g3 = new Game();
        g3.getPlayers().add(new Player("A"));
        g3.getPlayers().add(new Player("B"));
        g3.getPlayers().add(new Player("C"));
        gameEngineService.initializeGame(g3);

        assertEquals(5, g3.getBankCoins()[0], "3-player: 5 gems each");
        assertEquals(4, g3.getActiveNobles().size(), "3-player: 4 nobles");
    }

    @Test
    void fourPlayerGameHasCorrectSetup() {
        Game g4 = new Game();
        g4.getPlayers().add(new Player("A"));
        g4.getPlayers().add(new Player("B"));
        g4.getPlayers().add(new Player("C"));
        g4.getPlayers().add(new Player("D"));
        gameEngineService.initializeGame(g4);

        assertEquals(7, g4.getBankCoins()[0], "4-player: 7 gems each");
        assertEquals(5, g4.getActiveNobles().size(), "4-player: 5 nobles");
    }

    @Test
    void turnRotatesAmongAllPlayers() {
        Game g3 = new Game();
        Player a = new Player("A");
        Player b = new Player("B");
        Player c = new Player("C");
        g3.getPlayers().add(a);
        g3.getPlayers().add(b);
        g3.getPlayers().add(c);
        gameEngineService.initializeGame(g3);

        assertEquals(a, g3.getCurrentPlayer());
        gameEngineService.changeTurns(g3);
        assertEquals(b, g3.getCurrentPlayer());
        gameEngineService.changeTurns(g3);
        assertEquals(c, g3.getCurrentPlayer());
        gameEngineService.changeTurns(g3);
        assertEquals(a, g3.getCurrentPlayer(), "Should wrap back to first player");
    }
}