package np.cincuentazo.model;

import np.cincuentazo.exception.EmptyDeckException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a standard 52-card deck.
 * Supports drawing, shuffling, and recycling cards from the table.
 */
public class Deck {

    private final LinkedList<Card> cards;

    /** Constructs a full, shuffled 52-card deck. */
    public Deck() {
        cards = new LinkedList<>(buildFullDeck());
        shuffle();
    }

    /** Package-private constructor used in unit tests with a predefined card list. */
    Deck(LinkedList<Card> cards) {
        this.cards = new LinkedList<>(cards);
    }

    private List<Card> buildFullDeck() {
        List<Card> deck = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck.add(new Card(suit, rank));
            }
        }
        return deck;
    }

    /** Shuffles all cards currently in the deck. */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * Draws and returns the top card from the deck.
     *
     * @return the top card
     * @throws EmptyDeckException if the deck has no cards remaining
     */
    public Card drawCard() {
        if (cards.isEmpty()) {
            throw new EmptyDeckException(
                    "Attempted to draw from an empty deck. " +
                    "Call refillFromTableCards() before drawing."
            );
        }
        return cards.pollFirst();
    }

    /**
     * Adds a list of cards face-down to the bottom of the deck.
     *
     * @param newCards the cards to add; ignored if {@code null} or empty
     */
    public void addCardsToBottom(List<Card> newCards) {
        if (newCards == null || newCards.isEmpty()) return;
        for (Card c : newCards) {
            c.setFaceUp(false);   // ← BUG FIX: was incorrectly calling drawCard() here,
            cards.addLast(c);     //   which removed a deck card for every card added.
        }
    }

    /**
     * Recycles all table cards except the last one back into the deck.
     * The recycled cards are shuffled before being added to the bottom.
     * The {@code tableCards} list is modified in place so that only the
     * last played card remains in it.
     *
     * @param tableCards the current list of table cards
     * @return the number of cards recycled into the deck
     * @throws IllegalArgumentException if {@code tableCards} is {@code null} or empty
     */
    public int refillFromTableCards(List<Card> tableCards) {
        if (tableCards == null || tableCards.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot refill deck: tableCards is null or empty."
            );
        }
        List<Card> toRecycle = new ArrayList<>(
                tableCards.subList(0, tableCards.size() - 1)
        );
        tableCards.removeAll(toRecycle);
        Collections.shuffle(toRecycle);
        addCardsToBottom(toRecycle);
        return toRecycle.size();
    }

    /**
     * Returns whether the deck has no cards remaining.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Returns the number of cards remaining in the deck.
     *
     * @return deck size
     */
    public int size() {
        return cards.size();
    }

    @Override
    public String toString() {
        return "Deck [remaining = " + cards.size() + "]";
    }
}
