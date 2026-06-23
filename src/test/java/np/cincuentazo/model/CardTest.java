package np.cincuentazo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.CsvSource;
//import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Card}, focused on value resolution logic.
 *
 * <h2>What this test class verifies</h2>
 * <ul>
 *   <li>Numeric cards (2–8, 10) return their face value.</li>
 *   <li>The 9 is neutral (value = 0).</li>
 *   <li>Face cards (J, Q, K) subtract 10.</li>
 *   <li>The Ace dynamically resolves to 1 or 10 depending on the table sum —
 *       this is the most error-prone rule in the whole game, so it gets
 *       the most thorough coverage.</li>
 *   <li>{@link Card#isPlayable(int)} correctly reflects whether a card
 *       would bust the table sum.</li>
 * </ul>
 *
 * <p>Tests are grouped with {@code @Nested} classes to keep related
 * assertions organized and to make failures easy to locate by category.</p>
 */

public class CardTest {

    // -------------------------------------------------------------------------
    // Numeric cards: 2-8 and 10
    // -------------------------------------------------------------------------


    @Nested
    @DisplayName("Numeric cards (2-8, 10")
    class NumericCards{
        @ParameterizedTest(name = "{0} should add {1} to tableSum")
        @CsvSource({
                "TWO, 2",
                "THREE, 3",
                "FOUR, 4",
                "FIVE, 5",
                "SIX, 6",
                "SEVEN, 7",
                "EIGHT, 8",
                "TEN, 10"
        })

        @DisplayName("Numeric ranks return their face value regardless of tableSum")
        void numericCardsReturnFaceValue(Rank rank, int expectedValue) {
            Card card = new Card(Suit.HEARTS, rank);
            // The tableSum should not affect numeric cards at all —
            // testing with two very different sums proves independence.
            assertEquals(expectedValue, card.getValue(0),
                    "Value should match face number when table sum is 0");
            assertEquals(expectedValue, card.getValue(30),
                    "Value should match face number when tableSum is 30");
        }
    }

    // -------------------------------------------------------------------------
    // The neutral 9
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("The 9 (neutral card)")
    class NineCard {
        @Test
        @DisplayName("Nine always returns 0, regardless of tableSum")

        void nineIsAlwaysNeutral() {
            Card nine = new Card(Suit.CLUBS, Rank.NINE);

            assertEquals(0, nine.getValue(0), "Nine should be 0 at sum = 0");
            assertEquals(0, nine.getValue(45), "Nine should be 0 at sum = 45");
            assertEquals(0, nine.getValue(-10), "Nine should be 0 even at a negative sum");
        }
    }

    // -------------------------------------------------------------------------
    // Face cards: J, Q, K
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Face cards (J, Q, K)")
    class FaceCards {
        @ParameterizedTest (name = "{0} should always subtract 10")
        @EnumSource(value = Rank.class, names= {"JACK", "QUEEN", "KING"})
        @DisplayName("Face cards always return -10, regardless of tableSum")
        void faceCardsAlwaysSubstractTen(Rank rank){
            Card card = new Card(Suit.SPADES, rank);

            assertEquals(-10, card.getValue(0));
            assertEquals(-10, card.getValue(40));
        }
    }

    // -------------------------------------------------------------------------
    // The Ace — the trickiest rule in the game
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("The Ace (dynamic 1 or 10)")
    class AceCard{
        @Test
        @DisplayName("Ace adds 10 when the tableSum allows it (tableSum + 10 <= 50)")
        void aceAddsTenWhenSafe(){
            Card ace = new Card(Suit.DIAMONDS, Rank.ACE);

            assertEquals(10, ace.getValue(30),
                    "Ace should resolve to 10 when doing so keeps de sum <= 50");
        }

        @Test
        @DisplayName("Ace adds exactly 10 at the boundary (sum = 40-> 50)")
        void aceAddsTenAtExactBoundary(){
            Card ace = new Card(Suit.DIAMONDS, Rank.ACE);

            assertEquals(10, ace.getValue(40),
                    "Ace should resolve to 10 when the result lands exactly on 50");
        }
        @Test
        @DisplayName("Ace falls back to 1 when adding 10 would exceed 50")
        void aceFallsBackToOneWhenTenWouldBust() {
            Card ace = new Card(Suit.DIAMONDS, Rank.ACE);

            // 45 + 10 = 55 -> illegal, must fall back to 1 (45 + 1 = 46, legal)
            assertEquals(1, ace.getValue(45),
                    "Ace should resolve to 1 when adding 10 would exceed 50");
        }
        @Test
        @DisplayName("Ace at sum=49 must fall back to 1 (49+10=59 busts, 49+1=50 is legal)")
        void aceAtFortyNineFallsBackToOne() {
            Card ace = new Card(Suit.DIAMONDS, Rank.ACE);

            assertEquals(1, ace.getValue(49));
        }

        @Test
        @DisplayName("Ace at sum=0 (initial table card) resolves to 10")
        void aceAsInitialTableCardResolvesToTen() {
            // This covers the "table can start at 1" scenario mentioned in
            // the rules — actually verifying it starts at 10, since 0+10<=50.
            Card ace = new Card(Suit.DIAMONDS, Rank.ACE);

            assertEquals(10, ace.getValue(0));
        }
    }
    // -------------------------------------------------------------------------
    // isPlayable() — the legality check used by Hand and MachinePlayer
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isPlayable(tableSum)")
    class PlayabilityChecks {

        @Test
        @DisplayName("A numeric card is playable when it would not exceed 50")
        void numericCardIsPlayableWhenSafe() {
            Card seven = new Card(Suit.HEARTS, Rank.SEVEN);
            assertTrue(seven.isPlayable(40), "40 + 7 = 47, within range");
        }

        @Test
        @DisplayName("A numeric card is NOT playable when it would exceed 50")
        void numericCardIsNotPlayableWhenItBusts() {
            Card eight = new Card(Suit.HEARTS, Rank.EIGHT);
            assertFalse(eight.isPlayable(45), "45 + 8 = 53, exceeds 50");
        }
        @Test
        @DisplayName("Face cards are always playable since they subtract")
        void faceCardsAreAlwaysPlayable() {
            Card king = new Card(Suit.SPADES, Rank.KING);
            assertTrue(king.isPlayable(50), "50 - 10 = 40, always safe");
        }

        @Test
        @DisplayName("Ace is playable even at sum=49, because it can fall back to 1")
        void aceIsPlayableEvenNearTheLimit() {
            Card ace = new Card(Suit.DIAMONDS, Rank.ACE);
            assertTrue(ace.isPlayable(49), "49 + 1 = 50, legal via fallback");
        }

        @Test
        @DisplayName("A card exactly at the 50 boundary is playable")
        void cardLandingExactlyOnFiftyIsPlayable() {
            Card five = new Card(Suit.HEARTS, Rank.FIVE);
            assertTrue(five.isPlayable(45), "45 + 5 = 50 exactly, allowed");
        }
    }

    // -------------------------------------------------------------------------
    // Card identity and visibility (bonus coverage)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Card identity and face state")
    class IdentityAndVisibility {

        @Test
        @DisplayName("A new card starts face down")
        void newCardStartsFaceDown() {
            Card card = new Card(Suit.CLUBS, Rank.FIVE);
            assertFalse(card.isFaceUp());
        }

        @Test
        @DisplayName("flip() toggles the face-up state")
        void flipTogglesFaceState() {
            Card card = new Card(Suit.CLUBS, Rank.FIVE);
            card.flip();
            assertTrue(card.isFaceUp());
            card.flip();
            assertFalse(card.isFaceUp());
        }

        @Test
        @DisplayName("Two cards with the same suit and rank are equal")
        void cardsWithSameSuitAndRankAreEqual() {
            Card a = new Card(Suit.HEARTS, Rank.QUEEN);
            Card b = new Card(Suit.HEARTS, Rank.QUEEN);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Cards with different ranks are not equal")
        void cardsWithDifferentRanksAreNotEqual() {
            Card a = new Card(Suit.HEARTS, Rank.QUEEN);
            Card b = new Card(Suit.HEARTS, Rank.KING);
            assertNotEquals(a, b);
        }
    }
}
