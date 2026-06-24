package np.cincuentazo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MachinePlayer}, focused on the AI card-selection
 * strategy implemented in {@link MachinePlayer#chooseBestCardIndex(int)}.
 *
 * <h2>What this test class verifies</h2>
 * <ul>
 *   <li>{@code chooseBestCardIndex()} selects the card whose play would leave
 *       the table sum as high as possible without exceeding 50 — i.e., the
 *       card that maximises pressure on the next player.</li>
 *   <li>Among multiple cards with the same resulting sum, any of them is
 *       acceptable (the test only checks the sum, not the specific card
 *       when there is a tie).</li>
 *   <li>The method throws {@link IllegalStateException} when the machine has
 *       no playable card — it is the caller's responsibility to check
 *       {@link Player#canPlay(int)} first.</li>
 *   <li>The method is read-only: calling it does not modify the hand.</li>
 * </ul>
 *
 * <h2>Testing strategy</h2>
 * <p>Each test builds a controlled hand directly via
 * {@link Player#drawCard(Card)} so that the choice is deterministic and
 * independent of deck-shuffling in {@link GameState}.</p>
 */
public class MachinePlayerTest {

    // =========================================================================
    // chooseBestCardIndex() — correct selection
    // =========================================================================

    @Nested
    @DisplayName("chooseBestCardIndex() — strategy selection")
    class BestCardSelection {

        @Test
        @DisplayName("Picks the card that leaves the highest resulting sum (all different)")
        void picksCardWithHighestResultingSum() {
            MachinePlayer machine = new MachinePlayer("Máquina 1");
            // tableSum = 40
            // THREE  → 40+3  = 43
            // FIVE   → 40+5  = 45  ← best
            // KING   → 40-10 = 30
            machine.drawCard(new Card(Suit.CLUBS,    Rank.THREE));
            machine.drawCard(new Card(Suit.HEARTS,   Rank.FIVE));
            machine.drawCard(new Card(Suit.SPADES,   Rank.KING));

            int idx = machine.chooseBestCardIndex(40);
            Card chosen = machine.getHand().getCard(idx);

            // The chosen card must produce the highest result (45 in this case)
            int resultingSum = 40 + chosen.getValue(40);
            assertEquals(45, resultingSum,
                    "Machine must pick FIVE (40+5=45), the card leaving the highest sum");
        }

        @Test
        @DisplayName("Picks the only playable card when just one is legal")
        void picksOnlyPlayableCard() {
            MachinePlayer machine = new MachinePlayer("Máquina 1");
            // tableSum = 45
            // EIGHT → 45+8  = 53 ✗ not playable
            // SEVEN → 45+7  = 52 ✗ not playable
            // KING  → 45-10 = 35 ✓ only legal option
            machine.drawCard(new Card(Suit.CLUBS,  Rank.EIGHT));
            machine.drawCard(new Card(Suit.HEARTS, Rank.SEVEN));
            machine.drawCard(new Card(Suit.SPADES, Rank.KING));

            int idx = machine.chooseBestCardIndex(45);
            Card chosen = machine.getHand().getCard(idx);

            assertEquals(new Card(Suit.SPADES, Rank.KING), chosen,
                    "When only KING is playable, it must be chosen");
        }

        @Test
        @DisplayName("Picks card that lands exactly on 50 when that is the best legal option")
        void picksCardLandingExactlyOnFifty() {
            MachinePlayer machine = new MachinePlayer("Máquina 1");
            // tableSum = 45
            // FIVE  → 45+5  = 50 ✓ best (exact boundary)
            // THREE → 45+3  = 48 ✓ legal but not best
            machine.drawCard(new Card(Suit.HEARTS, Rank.FIVE));
            machine.drawCard(new Card(Suit.CLUBS,  Rank.THREE));

            int idx = machine.chooseBestCardIndex(45);
            Card chosen = machine.getHand().getCard(idx);

            assertEquals(new Card(Suit.HEARTS, Rank.FIVE), chosen,
                    "FIVE lands exactly on 50 — machine must prefer the boundary over a lower result");
        }

        @Test
        @DisplayName("Prefers Ace as 10 when that yields a higher (but still legal) sum")
        void prefersAceAsTenWhenSafe() {
            MachinePlayer machine = new MachinePlayer("Máquina 1");
            // tableSum = 35
            // ACE  → 35+10 = 45 ✓ (10 is used because 35+10 <= 50)  ← best
            // TWO  → 35+2  = 37 ✓ legal but lower
            machine.drawCard(new Card(Suit.DIAMONDS, Rank.ACE));
            machine.drawCard(new Card(Suit.HEARTS,   Rank.TWO));

            int idx = machine.chooseBestCardIndex(35);
            Card chosen = machine.getHand().getCard(idx);

            assertEquals(new Card(Suit.DIAMONDS, Rank.ACE), chosen,
                    "Ace resolves to 10 at sum=35 (35+10=45), which is higher than TWO (35+2=37)");
        }

        @Test
        @DisplayName("Returns a valid hand index (0-based, within hand bounds)")
        void returnedIndexIsWithinBounds() {
            MachinePlayer machine = new MachinePlayer("Máquina 1");
            machine.drawCard(new Card(Suit.HEARTS, Rank.FOUR));
            machine.drawCard(new Card(Suit.CLUBS,  Rank.SIX));
            machine.drawCard(new Card(Suit.SPADES, Rank.TWO));
            machine.drawCard(new Card(Suit.DIAMONDS, Rank.NINE));

            int idx = machine.chooseBestCardIndex(20);

            assertTrue(idx >= 0 && idx < machine.getHand().getHandSize(),
                    "Returned index must be within the valid range of the hand");
        }
    }

    // =========================================================================
    // chooseBestCardIndex() — no playable cards
    // =========================================================================

    @Nested
    @DisplayName("chooseBestCardIndex() — no playable cards")
    class NoPlayableCards {

        @Test
        @DisplayName("Throws IllegalStateException when no card is playable")
        void throwsWhenNoCardIsPlayable() {
            MachinePlayer machine = new MachinePlayer("Máquina 1");
            // tableSum = 49 — only -10 cards (J/Q/K) or Ace (as 1 → 50) would be legal.
            // Fill the hand with cards that all bust at sum=49.
            machine.drawCard(new Card(Suit.HEARTS,  Rank.TWO));    // 49+2  = 51 ✗
            machine.drawCard(new Card(Suit.CLUBS,   Rank.THREE));  // 49+3  = 52 ✗
            machine.drawCard(new Card(Suit.SPADES,  Rank.SEVEN));  // 49+7  = 56 ✗
            machine.drawCard(new Card(Suit.DIAMONDS, Rank.TEN));   // 49+10 = 59 ✗

            assertThrows(IllegalStateException.class,
                    () -> machine.chooseBestCardIndex(49),
                    "Must throw IllegalStateException when no card can be played");
        }

        @Test
        @DisplayName("Exception message mentions the machine's name")
        void exceptionMessageMentionsPlayerName() {
            MachinePlayer machine = new MachinePlayer("Máquina X");
            machine.drawCard(new Card(Suit.HEARTS, Rank.TEN));  // 49+10=59 ✗

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> machine.chooseBestCardIndex(49));
            assertTrue(ex.getMessage().contains("Máquina X"),
                    "Exception message should identify which machine had no playable card");
        }
    }

    // =========================================================================
    // chooseBestCardIndex() — read-only guarantee
    // =========================================================================

    @Nested
    @DisplayName("chooseBestCardIndex() — does not modify the hand")
    class ReadOnlyGuarantee {

        @Test
        @DisplayName("Hand size is unchanged after calling chooseBestCardIndex()")
        void handSizeUnchangedAfterCall() {
            MachinePlayer machine = new MachinePlayer("Máquina 1");
            machine.drawCard(new Card(Suit.HEARTS,   Rank.FIVE));
            machine.drawCard(new Card(Suit.CLUBS,    Rank.THREE));
            machine.drawCard(new Card(Suit.SPADES,   Rank.KING));
            machine.drawCard(new Card(Suit.DIAMONDS, Rank.ACE));

            int sizeBefore = machine.getHand().getHandSize();
            machine.chooseBestCardIndex(30);
            int sizeAfter  = machine.getHand().getHandSize();

            assertEquals(sizeBefore, sizeAfter,
                    "chooseBestCardIndex() is read-only and must not remove any card from the hand");
        }
    }
}
