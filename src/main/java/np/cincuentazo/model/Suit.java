package np.cincuentazo.model;
/**
 * Represents the four suits of a standard poker deck.
 *
 * <p>Each suit has a display symbol used for UI rendering and logging.
 * The suit does not affect game logic in Cincuentazo — only the rank
 * determines a card's value — but is required to form a complete 52-card deck
 * and to distinguish visually identical cards (e.g., Ace of Hearts vs Ace of Spades).</p>
 */

public enum Suit {

    HEARTS("♥"),
    DIAMONDS ("♦"),
    CLUBS ("♣"),
    SPADES ("♠");

    private final String symbol;
    /**
     * Associates a display symbol with this suit.
     *
     * @param symbol the Unicode character representing this suit
     */

    Suit (String symbol){
        this.symbol = symbol;
    }

    /**
     * Returns the Unicode symbol for this suit (e.g., {@code "♥"} for HEARTS).
     *
     * @return a single-character string containing the suit symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns a human-readable representation including the suit name and symbol.
     * Example: {@code "HEARTS ♥"}
     *
     * @return formatted string for debugging and logging
     */
    @Override
    public String toString(){
        return name()+ " " + symbol;
    }
}
