package np.cincuentazo.model;

import np.cincuentazo.exception.EmptyDeckException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Deck}.
 *
 * <h2>What this test class verifies</h2>
 * <ul>
 *   <li>A new deck always has exactly 52 unique cards.</li>
 *   <li>{@link Deck#drawCard()} reduces the deck size by one each call.</li>
 *   <li>{@link Deck#drawCard()} throws {@link EmptyDeckException} once empty.</li>
 *   <li>{@link Deck#shuffle()} reorders cards without losing or duplicating any.</li>
 *   <li>{@link Deck#refillFromTableCards(List)} correctly recycles all table
 *       cards except the last one, and leaves the table sum's "last card"
 *       untouched.</li>
 *   <li>{@link Deck#addCardsToBottom(List)} appends cards and flips them face down.</li>
 * </ul>
 */
public class DeckTest {

    private Deck deck;

    @BeforeEach
    void setUp(){
        deck = new Deck();
    }

    // -------------------------------------------------------------------------
    // Deck construction
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Deck construction")
    class Construction {
        @Test
        @DisplayName("A new deck has exactly 52 cards")
        void newDeckHasFiftyTwoCards() {
            assertEquals(52, deck.size());
        }

        @Test
        @DisplayName("A new deck is not empty")
        void newDeckIsNotEmpty() {
            assertFalse(deck.isEmpty());
        }

        @Test
        @DisplayName("A new deck contains all 52 unique suit-rank combinations")
        void newDeckContainsAllUniqueCards() {
            Set<Card> uniqueCards = new HashSet<>();
            while (!deck.isEmpty()) {
                uniqueCards.add(deck.drawCard());
            }

            // If any card were duplicated, the set would have fewer than 52 entries
            // because Card.equals() is based on (suit, rank).
            assertEquals(52, uniqueCards.size(),
                    "All 52 cards should be unique combinations of suit and rank");
        }
    }

    // -------------------------------------------------------------------------
    // drawCard()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("drawCard()")
    class DrawingCards {

        @Test
        @DisplayName("Drawing one card reduces the deck size by exactly one")
        void drawReducesSizeByOne() {
            int sizeBefore = deck.size();
            deck.drawCard();
            assertEquals(sizeBefore - 1, deck.size());
        }

        @Test
        @DisplayName("Drawing all 52 cards empties the deck")
        void drawingAllCardsEmptiesDeck() {
            for (int i = 0; i < 52; i++) {
                deck.drawCard();
            }
            assertTrue(deck.isEmpty());
            assertEquals(0, deck.size());
        }

        @Test
        @DisplayName("Drawing from an empty deck throws EmptyDeckException")
        void drawingFromEmptyDeckThrows() {
            // Exhaust the deck first
            for (int i = 0; i < 52; i++) {
                deck.drawCard();
            }

            assertThrows(EmptyDeckException.class, deck::drawCard,
                    "Drawing from an empty deck must throw EmptyDeckException");
        }

        @Test
        @DisplayName("Drawn cards are never null")
        void drawnCardsAreNeverNull() {
            Card card = deck.drawCard();
            assertNotNull(card);
        }
    }

    // -------------------------------------------------------------------------
    // shuffle()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("shuffle()")
    class Shuffling {

        @Test
        @DisplayName("Shuffle preserves the total card count")
        void shufflePreservesCardCount() {
            int sizeBefore = deck.size();
            deck.shuffle();
            assertEquals(sizeBefore, deck.size());
        }

        @Test
        @DisplayName("Shuffle does not lose or duplicate any card")
        void shuffleDoesNotLoseOrDuplicateCards() {
            // Capture all cards before shuffling by draining a fresh, unshuffled deck.
            List<Card> before = drainAllCards(new Deck());

            Deck toShuffle = new Deck();
            toShuffle.shuffle();
            List<Card> after = drainAllCards(toShuffle);

            // Order may differ, but the set of cards must be identical.
            assertEquals(new HashSet<>(before), new HashSet<>(after),
                    "Shuffling must not change which cards exist in the deck");
        }

        /** Helper: draws every card from a deck into a list. */
        private List<Card> drainAllCards(Deck d) {
            List<Card> result = new ArrayList<>();
            while (!d.isEmpty()) {
                result.add(d.drawCard());
            }
            return result;
        }
    }


    // -------------------------------------------------------------------------
    // addCardsToBottom()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addCardsToBottom(list)")
    class AddingCardsToBottom {
        @Test
        @DisplayName("Adding cards increases the deck size accordingly")
        void addingCardsIncreasesSize(){
            // Empty the deck completely first for a clean count

            while (!deck.isEmpty()) deck.drawCard();

            List<Card> returned = List.of(
                    new Card(Suit.HEARTS, Rank.FIVE),
                    new Card(Suit.CLUBS, Rank.KING)
            );

            deck.addCardsToBottom(returned);

            assertEquals(2, deck.size());
        }

        @Test
        @DisplayName("Cards added to the bottom are flipped face down")
        void addedCardsAreFlippedFaceDown() {
            while (!deck.isEmpty()) deck.drawCard();

            Card faceUpCard = new Card(Suit.HEARTS, Rank.FIVE);
            faceUpCard.setFaceUp(true);

            deck.addCardsToBottom(List.of(faceUpCard));
            Card drawnBack = deck.drawCard();

            assertFalse(drawnBack.isFaceUp(),
                    "Cards re-entering the deck must be face down, even if they were face up before");

        }
    }

    // -------------------------------------------------------------------------
    // refillFromTableCards()
    // -------------------------------------------------------------------------


    @Nested
    @DisplayName("refillFromTableCards(List)")
    class RefillingFromTable {

        @Test
        @DisplayName("Refill recycles all table cards except the last one")
        void refillKeepsOnlyLastTableCard() {
            while (!deck.isEmpty()) deck.drawCard();

            List<Card> tableCards = new ArrayList<>(List.of(
                    new Card(Suit.HEARTS, Rank.FIVE),   // played first
                    new Card(Suit.CLUBS, Rank.SEVEN),   // played second
                    new Card(Suit.SPADES, Rank.KING)    // played last -> must stay
            ));

            int recycled = deck.refillFromTableCards(tableCards);

            assertEquals(2, recycled, "Should recycle all cards except the last one played");
            assertEquals(2, deck.size(), "Deck should now contain the 2 recycled cards");
            assertEquals(1, tableCards.size(), "Only the last played card should remain on the table");
            assertEquals(new Card(Suit.SPADES, Rank.KING), tableCards.get(0),
                    "The remaining table card must be the one that was last played");
        }

        @Test
        @DisplayName("Refilling with only one table card recycles nothing")
        void refillWithSingleCardRecyclesNothing() {
            while (!deck.isEmpty()) deck.drawCard();

            List<Card> tableCards = new ArrayList<>(List.of(
                    new Card(Suit.HEARTS, Rank.NINE)
            ));

            int recycled = deck.refillFromTableCards(tableCards);

            assertEquals(0, recycled, "With only the last card present, nothing should be recycled");
            assertEquals(1, tableCards.size(), "The single table card must remain");
        }

        @Test
        @DisplayName("Refilling with an empty list throws IllegalArgumentException")
        void refillWithEmptyListThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> deck.refillFromTableCards(new ArrayList<>()));
        }

        @Test
        @DisplayName("Refilling with a null list throws IllegalArgumentException")
        void refillWithNullListThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> deck.refillFromTableCards(null));
        }

    }

}
