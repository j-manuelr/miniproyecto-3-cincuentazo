package np.cincuentazo.model;

import np.cincuentazo.exception.InvalidPlayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Hand}.
 *
 * <h2>What this test class verifies</h2>
 * <ul>
 *   <li>{@link Hand#removeCard(int, int)} throws {@link IndexOutOfBoundsException}
 *       when the index is out of range.</li>
 *   <li>{@link Hand#removeCard(int, int)} throws {@link InvalidPlayException}
 *       when the selected card is not legally playable.</li>
 *   <li>{@link Hand#getPlayableCards(int)} returns exactly the subset of cards
 *       that would not push the sum over 50.</li>
 *   <li>{@link Hand#canPlay(int)} mirrors the emptiness of
 *       {@code getPlayableCards}.</li>
 *   <li>{@link Hand#removeAll()} empties the hand and returns all cards.</li>
 * </ul>
 *
 * <p>Tests use controlled hands built manually rather than going through
 * {@link GameState}, so results are deterministic regardless of shuffle
 * order.</p>
 */
public class HandTest {

    /** A hand populated before each test with a predictable set of cards. */
    private Hand hand;

    /**
     * Populates the hand with four cards covering the main rule categories:
     * numeric (FIVE, EIGHT), face/subtract (KING), and Ace (dynamic value).
     * Table sum used in most tests is 45, so:
     * <ul>
     *   <li>FIVE  → 45+5  = 50 ✓ playable</li>
     *   <li>KING  → 45−10 = 35 ✓ playable</li>
     *   <li>ACE   → 45+1  = 46 ✓ playable (falls back to 1)</li>
     *   <li>EIGHT → 45+8  = 53 ✗ not playable</li>
     * </ul>
     */
    @BeforeEach
    void setUp() {
        hand = new Hand();
        hand.addCard(new Card(Suit.HEARTS,   Rank.FIVE));   // index 0 — playable at 45
        hand.addCard(new Card(Suit.SPADES,   Rank.KING));   // index 1 — playable at 45
        hand.addCard(new Card(Suit.DIAMONDS, Rank.ACE));    // index 2 — playable at 45
        hand.addCard(new Card(Suit.CLUBS,    Rank.EIGHT));  // index 3 — NOT playable at 45
    }

    // =========================================================================
    // removeCard() — out-of-range index
    // =========================================================================

    @Nested
    @DisplayName("removeCard() — index validation")
    class RemoveCardIndexValidation {

        @Test
        @DisplayName("removeCard() with a negative index throws IndexOutOfBoundsException")
        void negativeIndexThrows() {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> hand.removeCard(-1, 45),
                    "Negative index must throw IndexOutOfBoundsException");
        }

        @Test
        @DisplayName("removeCard() with index equal to hand size throws IndexOutOfBoundsException")
        void indexEqualToSizeThrows() {
            // hand has 4 cards → valid indices are 0-3; index 4 is out of bounds
            assertThrows(IndexOutOfBoundsException.class,
                    () -> hand.removeCard(4, 45),
                    "Index == handSize must throw IndexOutOfBoundsException");
        }

        @Test
        @DisplayName("removeCard() with index greater than hand size throws IndexOutOfBoundsException")
        void indexGreaterThanSizeThrows() {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> hand.removeCard(100, 45),
                    "Index far beyond hand size must throw IndexOutOfBoundsException");
        }

        @Test
        @DisplayName("removeCard() with a valid index on a playable card succeeds")
        void validIndexOnPlayableCardSucceeds() throws InvalidPlayException {
            // FIVE at index 0 is playable at tableSum=45 (45+5=50)
            Card removed = hand.removeCard(0, 45);
            assertEquals(new Card(Suit.HEARTS, Rank.FIVE), removed,
                    "Should return the card that was at the given index");
            assertEquals(3, hand.getHandSize(),
                    "Hand size should decrease by one after a successful remove");
        }
    }

    // =========================================================================
    // removeCard() — unplayable card
    // =========================================================================

    @Nested
    @DisplayName("removeCard() — playability validation")
    class RemoveCardPlayabilityValidation {

        @Test
        @DisplayName("removeCard() on a card that would bust throws InvalidPlayException")
        void unplayableCardThrowsInvalidPlayException() {
            // EIGHT at index 3: 45+8 = 53 > 50 → must throw
            assertThrows(InvalidPlayException.class,
                    () -> hand.removeCard(3, 45),
                    "Playing a card that busts the sum must throw InvalidPlayException");
        }

        @Test
        @DisplayName("InvalidPlayException carries the rejected card")
        void exceptionCarriesAttemptedCard() {
            Card eight = hand.getCard(3);
            InvalidPlayException ex = assertThrows(InvalidPlayException.class,
                    () -> hand.removeCard(3, 45));
            assertEquals(eight, ex.getAttemptedCard(),
                    "Exception must expose the card that caused the violation");
        }

        @Test
        @DisplayName("InvalidPlayException carries the table sum at the time of the attempt")
        void exceptionCarriesTableSum() {
            InvalidPlayException ex = assertThrows(InvalidPlayException.class,
                    () -> hand.removeCard(3, 45));
            assertEquals(45, ex.getTableSum(),
                    "Exception must expose the table sum that was in effect");
        }

        @Test
        @DisplayName("Hand is unchanged after a rejected play attempt")
        void handUnchangedAfterRejectedPlay() {
            assertThrows(InvalidPlayException.class,
                    () -> hand.removeCard(3, 45));
            assertEquals(4, hand.getHandSize(),
                    "A rejected play must not remove the card from the hand");
        }
    }

    // =========================================================================
    // getPlayableCards()
    // =========================================================================

    @Nested
    @DisplayName("getPlayableCards(tableSum)")
    class GetPlayableCards {

        @Test
        @DisplayName("Returns only cards that keep the sum <= 50 at tableSum=45")
        void returnsOnlyLegalCardsAtFortyFive() {
            // At tableSum=45: FIVE(+5=50✓), KING(-10=35✓), ACE(+1=46✓), EIGHT(+8=53✗)
            List<Card> playable = hand.getPlayableCards(45);

            assertEquals(3, playable.size(),
                    "Exactly 3 of 4 cards should be playable at sum=45");
            assertTrue(playable.contains(new Card(Suit.HEARTS,   Rank.FIVE)));
            assertTrue(playable.contains(new Card(Suit.SPADES,   Rank.KING)));
            assertTrue(playable.contains(new Card(Suit.DIAMONDS, Rank.ACE)));
            assertFalse(playable.contains(new Card(Suit.CLUBS,   Rank.EIGHT)),
                    "EIGHT must NOT be in the playable list at sum=45");
        }

        @Test
        @DisplayName("Returns all cards when the sum is low enough for all of them")
        void returnsAllCardsAtLowSum() {
            // At tableSum=0 every card is playable (highest would be EIGHT at 8)
            List<Card> playable = hand.getPlayableCards(0);
            assertEquals(hand.getHandSize(), playable.size(),
                    "All cards should be playable when the table sum is 0");
        }

        @Test
        @DisplayName("Returns an empty list when no card is playable")
        void returnsEmptyListWhenNothingPlayable() {
            // Build a hand where every card busts at sum=49
            Hand highHand = new Hand();
            highHand.addCard(new Card(Suit.HEARTS,  Rank.TWO));    // 49+2=51 ✗
            highHand.addCard(new Card(Suit.CLUBS,   Rank.THREE));  // 49+3=52 ✗
            highHand.addCard(new Card(Suit.SPADES,  Rank.TEN));    // 49+10=59 ✗
            highHand.addCard(new Card(Suit.DIAMONDS, Rank.EIGHT)); // 49+8=57 ✗

            assertTrue(highHand.getPlayableCards(49).isEmpty(),
                    "No card should be playable when every card busts the sum");
        }

        @Test
        @DisplayName("getPlayableCards() does not modify the hand")
        void doesNotModifyHand() {
            int sizeBefore = hand.getHandSize();
            hand.getPlayableCards(45);
            assertEquals(sizeBefore, hand.getHandSize(),
                    "getPlayableCards() is read-only and must not remove cards");
        }
    }

    // =========================================================================
    // canPlay()
    // =========================================================================

    @Nested
    @DisplayName("canPlay(tableSum)")
    class CanPlay {

        @Test
        @DisplayName("Returns true when at least one card is playable")
        void returnsTrueWhenAnyCardIsPlayable() {
            assertTrue(hand.canPlay(45),
                    "Hand has 3 playable cards at sum=45, so canPlay() must return true");
        }

        @Test
        @DisplayName("Returns false when no card can be played")
        void returnsFalseWhenNoCardIsPlayable() {
            Hand blockedHand = new Hand();
            blockedHand.addCard(new Card(Suit.HEARTS,  Rank.TWO));
            blockedHand.addCard(new Card(Suit.CLUBS,   Rank.THREE));
            blockedHand.addCard(new Card(Suit.SPADES,  Rank.TEN));
            blockedHand.addCard(new Card(Suit.DIAMONDS, Rank.EIGHT));

            assertFalse(blockedHand.canPlay(49),
                    "None of these cards can be played at sum=49");
        }

        @Test
        @DisplayName("canPlay() is consistent with getPlayableCards() — both empty or both non-empty")
        void consistentWithGetPlayableCards() {
            int[] sums = {0, 10, 30, 45, 49};
            for (int sum : sums) {
                boolean hasPlayable = !hand.getPlayableCards(sum).isEmpty();
                assertEquals(hasPlayable, hand.canPlay(sum),
                        "canPlay() and getPlayableCards().isEmpty() must agree at sum=" + sum);
            }
        }
    }

    // =========================================================================
    // removeAll()
    // =========================================================================

    @Nested
    @DisplayName("removeAll()")
    class RemoveAll {

        @Test
        @DisplayName("removeAll() returns all cards that were in the hand")
        void returnsAllCards() {
            List<Card> original = List.copyOf(hand.getCards());
            List<Card> removed  = hand.removeAll();

            assertEquals(original.size(), removed.size(),
                    "removeAll() must return the same number of cards that were in the hand");
            assertTrue(removed.containsAll(original),
                    "removeAll() must return exactly the cards that were in the hand");
        }

        @Test
        @DisplayName("removeAll() leaves the hand empty")
        void handIsEmptyAfterRemoveAll() {
            hand.removeAll();
            assertTrue(hand.isEmpty(),
                    "Hand must be empty after removeAll()");
            assertEquals(0, hand.getHandSize(),
                    "Hand size must be 0 after removeAll()");
        }

        @Test
        @DisplayName("removeAll() on an already-empty hand returns an empty list")
        void emptyHandReturnsEmptyList() {
            Hand empty = new Hand();
            List<Card> result = empty.removeAll();
            assertNotNull(result);
            assertTrue(result.isEmpty(),
                    "removeAll() on an empty hand must return an empty list, not null");
        }

        @Test
        @DisplayName("Returned list is independent — modifying it does not affect the hand")
        void returnedListIsIndependent() {
            List<Card> removed = hand.removeAll();
            // Adding something to the returned list must not resurrect cards in the hand
            removed.add(new Card(Suit.HEARTS, Rank.TWO));
            assertEquals(0, hand.getHandSize(),
                    "Modifying the returned list must not affect the (now empty) hand");
        }
    }
}
