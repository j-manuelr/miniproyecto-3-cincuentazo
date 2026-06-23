package np.cincuentazo.model;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameState}.
 *
 * <h2>What this test class verifies</h2>
 * <ul>
 *   <li>Construction deals 4 cards to each player and seeds the table sum.</li>
 *   <li>{@link GameState#isGameOver()} returns {@code true} only when one
 *       or zero players remain alive.</li>
 *   <li>{@link GameState#getWinner()} correctly identifies the sole survivor.</li>
 *   <li>{@link GameState#advanceTurn()} skips eliminated players and wraps
 *       around the player list circularly.</li>
 *   <li>{@link GameState#eliminatedCurrentPlayer()} marks the player as not
 *       alive and returns their cards to the deck.</li>
 * </ul>
 *
 * <h2>Testing strategy</h2>
 * <p>{@code GameState} deals cards randomly in its constructor, so tests
 * that need a specific table sum (e.g., for {@code canPlay} checks) cannot
 * rely on the dealt table card. Instead, those tests verify {@code canPlay}
 * logic at the {@code Hand}/{@code Player} level using controlled hands,
 * while {@code GameState}-level tests focus on what IS deterministic:
 * player count, turn rotation, and game-over detection.</p>
 */

public class GameStateTest {

    private List<Player> players;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        players = new ArrayList<>();
        players.add(new HumanPlayer("Tú"));
        players.add(new MachinePlayer("Máquina 1"));
        players.add(new MachinePlayer("Máquina 2"));
        gameState = new GameState(players);
    }

    // -------------------------------------------------------------------------
    // Construction / setup
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Initial setup")
    class InitialSetup {

        @Test
        @DisplayName("Each player is dealt exactly 4 cards at the start")
        void eachPlayerStartsWithFourCards() {
            for (Player p : gameState.getPlayers()) {
                assertEquals(Hand.HAND_SIZE, p.getHand().getHandSize(),
                        p.getName() + " should start with exactly 4 cards");
            }
        }

        @Test
        @DisplayName("The human player's cards are face up after dealing")
        void humanCardsAreFaceUpAfterDealing() {
            Player human = gameState.getPlayers().get(0);
            for (Card card : human.getHand().getCards()) {
                assertTrue(card.isFaceUp(), "Human's cards should be visible");
            }
        }

        @Test
        @DisplayName("Machine players' cards remain face down after dealing")
        void machineCardsAreFaceDownAfterDealing() {
            for (Player p : gameState.getPlayers()) {
                if (p instanceof MachinePlayer) {
                    for (Card card : p.getHand().getCards()) {
                        assertFalse(card.isFaceUp(), p.getName() + "'s cards should be hidden");
                    }
                }
            }
        }

        @Test
        @DisplayName("The table starts with exactly one card")
        void tableStartsWithOneCard() {
            assertEquals(1, gameState.getTableCardsCount());
            assertNotNull(gameState.getTopTableCard());
        }

        @Test
        @DisplayName("The human player goes first")
        void humanPlayerGoesFirst() {
            assertEquals(0, gameState.getCurrentTurnIndex());
            assertInstanceOf(HumanPlayer.class, gameState.getCurrentPlayer());
        }

        @Test
        @DisplayName("Constructing with fewer than 2 players throws")
        void constructingWithTooFewPlayersThrows() {
            List<Player> onePlayer = List.of(new HumanPlayer("Solo"));
            assertThrows(IllegalArgumentException.class, () -> new GameState(onePlayer));
        }

        @Test
        @DisplayName("Constructing with more than 4 players throws")
        void constructingWithTooManyPlayersThrows() {
            List<Player> fivePlayers = List.of(
                    new HumanPlayer("Tú"),
                    new MachinePlayer("M1"), new MachinePlayer("M2"),
                    new MachinePlayer("M3"), new MachinePlayer("M4")
            );
            assertThrows(IllegalArgumentException.class, () -> new GameState(fivePlayers));
        }
    }

    // -------------------------------------------------------------------------
    // isGameOver() and getWinner()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isGameOver() and getWinner()")
    class GameOverDetection {

        @Test
        @DisplayName("Game is not over while 3 players are alive")
        void gameIsNotOverWithThreeAlivePlayers() {
            assertFalse(gameState.isGameOver());
            assertNull(gameState.getWinner(), "There should be no winner while the game is ongoing");
        }

        @Test
        @DisplayName("Game is not over with exactly 2 players alive")
        void gameIsNotOverWithTwoAlivePlayers() {
            //Eliminate one of the three players, leaving 2 alive
            gameState.eliminatedCurrentPlayer(); // eliminates the human (current player)
            assertFalse(gameState.isGameOver(), "With 2 players still alive, the game continue");
        }

        @Test
        @DisplayName("Game is over once only one player remains alive")
        void gameIsOverWithOnlyOnePlayerAlive() {
            // Eliminate two of the three players.
            gameState.eliminatedCurrentPlayer();      // eliminate human (index 0)
            gameState.advanceTurn();                  // move to next alive player
            gameState.eliminatedCurrentPlayer();       // eliminate that one too

            assertTrue(gameState.isGameOver(),
                    "With only 1 player alive out of 3, the game should be over");
        }

        @Test
        @DisplayName("getWinner() returns the sole survivor once the game is over")
        void getWinnerReturnsTheSurvivor() {
            // Eliminate the human and one machine, leaving exactly one machine alive.
            Player human = gameState.getPlayers().get(0);
            gameState.eliminatedCurrentPlayer(); // eliminates human
            gameState.advanceTurn();
            gameState.eliminatedCurrentPlayer(); // eliminates the next alive player

            Player winner = gameState.getWinner();
            assertNotNull(winner);
            assertTrue(winner.isAlive());
            assertNotEquals(human, winner, "The eliminated human cannot be the winner");
        }
    }

    // -------------------------------------------------------------------------
    // advanceTurn()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("advanceTurn()")
    class TurnAdvancing {

        @Test
        @DisplayName("advanceTurn moves to the next player in order")
        void advanceTurnMovesToNextPlayer() {
            assertEquals(0, gameState.getCurrentTurnIndex());
            gameState.advanceTurn();
            assertEquals(1, gameState.getCurrentTurnIndex());
        }

        @Test
        @DisplayName("advanceTurn wraps around to index 0 after the last player")
        void advanceTurnWrapsAroundCircularly() {
            gameState.advanceTurn(); // 0 -> 1
            gameState.advanceTurn(); // 1 -> 2
            gameState.advanceTurn(); // 2 -> wraps to 0

            assertEquals(0, gameState.getCurrentTurnIndex());
        }

        @Test
        @DisplayName("advanceTurn skips eliminated players")
        void advanceTurnSkipsEliminatedPlayers() {
            // Eliminate the player at index 1 (Máquina 1).
            gameState.advanceTurn();              // move to index 1
            gameState.eliminatedCurrentPlayer();    // eliminate Máquina 1
            gameState.advanceTurn();               // should skip the eliminated player...

            // advanceTurn() starts FROM currentTurnIndex (still 1, now dead)
            // and looks for the next alive player, landing on index 2.
            assertEquals(2, gameState.getCurrentTurnIndex(),
                    "Should skip the eliminated player at index 1 and land on index 2");
        }
    }

    // -------------------------------------------------------------------------
    // eliminateCurrentPlayer()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("eliminatedCurrentPlayer()")
    class Elimination {

        @Test
        @DisplayName("Eliminating the current player marks them as not alive")
        void eliminationMarksPlayerAsNotAlive() {
            Player current = gameState.getCurrentPlayer();
            gameState.eliminatedCurrentPlayer();

            assertFalse(current.isAlive());
        }

        @Test
        @DisplayName("Eliminating the current player empties their hand")
        void eliminationEmptiesTheHand() {
            Player current = gameState.getCurrentPlayer();
            gameState.eliminatedCurrentPlayer();

            assertTrue(current.getHand().isEmpty(),
                    "All cards should be removed from an eliminated player's hand");
        }

        @Test
        @DisplayName("Eliminated player's cards are returned to the deck")
        void eliminatedCardsReturnToDeck() {
            int deckSizeBefore = gameState.getDeck().size();
            int handSize = gameState.getCurrentPlayer().getHand().getHandSize();

            gameState.eliminatedCurrentPlayer();

            assertEquals(deckSizeBefore + handSize, gameState.getDeck().size(),
                    "The eliminated player's cards should be added back to the deck");
        }
    }

    // canPlay() at the Player/Hand level (controlled, deterministic hands)

    @Nested
    @DisplayName("canPlay() with controlled hands")
    class CanPlayChecks {

        @Test
        @DisplayName("A player with only a King can still play when sum is high (subtracts)")
        void playerCanPlayKingNearTheLimit() {
            HumanPlayer player = new HumanPlayer("Test");
            player.drawCard(new Card(Suit.SPADES, Rank.KING));

            assertTrue(player.canPlay(50), "King subtracts 10, always legal even at sum=50");
        }

        @Test
        @DisplayName("A player with only a high numeric card cannot play when it would bust")
        void playerCannotPlayWhenOnlyCardBusts() {
            HumanPlayer player = new HumanPlayer("Test");
            player.drawCard(new Card(Suit.HEARTS, Rank.EIGHT));

            assertFalse(player.canPlay(45), "45 + 8 = 53 > 50, no legal move with only this card");
        }

        @Test
        @DisplayName("A player with an Ace can always play near the limit (falls back to 1)")
        void playerWithAceCanAlwaysPlayNearLimit() {
            HumanPlayer player = new HumanPlayer("Test");
            player.drawCard(new Card(Suit.HEARTS, Rank.ACE));

            assertTrue(player.canPlay(49), "Ace falls back to 1: 49+1=50, legal");
        }

        @Test
        @DisplayName("canPlay returns true if at least one of several cards is legal")
        void canPlayReturnsTrueIfAnyCardIsLegal() {
            HumanPlayer player = new HumanPlayer("Test");
            player.drawCard(new Card(Suit.HEARTS, Rank.EIGHT));  // illegal at sum=45
            player.drawCard(new Card(Suit.SPADES, Rank.KING));   // legal (subtracts)

            assertTrue(player.canPlay(45));
        }
    }

}