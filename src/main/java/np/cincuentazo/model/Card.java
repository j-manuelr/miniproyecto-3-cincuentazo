package np.cincuentazo.model;

import java.util.Objects;

/**
 * Represents a single playing card with a rank, suit, and face orientation.
 */
public class Card {

    public static final int MAX_TABLE_SUM = 50;

    private final Suit suit;
    private final Rank rank;
    private boolean faceUp;

    /**
     * Constructs a Card with the given suit and rank, initially face down.
     *
     * @param suit the card suit
     * @param rank the card rank
     */
    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
        faceUp = false;
    }

    /**
     * Returns the game value of this card given the current table sum.
     * The Ace returns 10 when it fits (sum+10 ≤ 50), otherwise returns 1.
     *
     * @param tableSum the current table sum before this card is played
     * @return the value to add or subtract from the table sum
     */
    public int getValue(int tableSum) {
        if (rank.isAce()) {
            return (tableSum + 10 <= MAX_TABLE_SUM) ? 10 : 1;
        }
        return rank.getBaseValue();
    }

    /**
     * Returns whether this card can be played without the table sum exceeding 50.
     *
     * @param tableSum the current table sum
     * @return {@code true} if the card is playable
     */
    public boolean isPlayable(int tableSum) {
        return tableSum + getValue(tableSum) <= MAX_TABLE_SUM;
    }

    /** Flips the face orientation of this card. */
    public void flip() {
        faceUp = !faceUp;
    }

    /**
     * Sets the face orientation of this card.
     *
     * @param faceUp {@code true} for face-up, {@code false} for face-down
     */
    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    /**
     * Returns whether this card is currently face up.
     *
     * @return {@code true} if face up
     */
    public boolean isFaceUp() {
        return faceUp;
    }

    /**
     * Returns the suit of this card.
     *
     * @return the suit
     */
    public Suit getSuit() {
        return suit;
    }

    /**
     * Returns the rank of this card.
     *
     * @return the rank
     */
    public Rank getRank() {
        return rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card other)) return false;
        return suit == other.suit && rank == other.rank;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Two cards are equal if and only if they share the same suit and rank.
     * Face-up state is intentionally excluded from equality because two cards
     * with the same identity are the same card regardless of orientation.
     *
     * <p>Note: in a standard deck there is exactly one card per (suit, rank)
     * combination, so equality should rarely be triggered between distinct
     * card objects in normal gameplay.</p>
     */
    @Override
    public int hashCode() {
        return Objects.hash(suit, rank);
    }

    /**
     * Returns a compact string representation of the card.
     * Example: {@code "A♥"}, {@code "10♣"}, {@code "J♠"}.
     *
     * @return rank label followed by suit symbol
     */
    @Override
    public String toString() {
        return rank.getLabel() + suit.getSymbol();
    }
}
