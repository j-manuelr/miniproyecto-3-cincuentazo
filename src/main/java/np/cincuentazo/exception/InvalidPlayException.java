package np.cincuentazo.exception;

import np.cincuentazo.model.Card;

/**
 * Thrown when a player attempts to play a card that would push the table sum
 * above the legal maximum of {@link np.cincuentazo.model.Card#MAX_TABLE_SUM} (50).
 *
 * <p>This is a <em>checked</em> exception because an invalid play is a
 * recoverable, player-visible condition: the game must catch it, display an
 * appropriate message, and let the player choose again (or, in the case of a
 * machine player, treat it as a programming error via the safety net in
 * {@link np.cincuentazo.controller.GameController#processMachinePlay}).</p>
 *
 * <h2>Carried context</h2>
 * <ul>
 *   <li>{@link #attemptedCard} — the card that was rejected, so the UI can
 *       highlight it or describe it in the error message.</li>
 *   <li>{@link #tableSum} — the table sum at the moment of the attempt, so
 *       the message can show exactly why the card was illegal.</li>
 * </ul>
 *
 * <p>The detail message is built automatically by
 * {@link #buildMessage(Card, int)} and always follows the format:
 * <pre>
 *   Cannot play A♠: table sum would become 55 (max allowed is 50). Current sum: 45.
 * </pre>
 * </p>
 */
public class InvalidPlayException extends Exception {

    /** The card whose play was rejected. Previously misspelled as {@code attempetedCard}. */
    private final Card attemptedCard;

    /** The table sum at the moment the illegal play was attempted. */
    private final int tableSum;

    /**
     * Constructs a new {@code InvalidPlayException} for the given card and table sum.
     *
     * @param attemptedCard the card the player tried to play; must not be {@code null}
     * @param tableSum      the running table sum at the time of the attempt
     */
    public InvalidPlayException(Card attemptedCard, int tableSum) {
        super(buildMessage(attemptedCard, tableSum));
        this.attemptedCard = attemptedCard;
        this.tableSum      = tableSum;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String buildMessage(Card card, int sum) {
        int resultingSum = sum + card.getValue(sum);
        return "Cannot play " + card + ": table sum would become " +
                resultingSum + " (max allowed is " + Card.MAX_TABLE_SUM + "). " +
                "Current sum: " + sum + ".";
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the card that triggered this exception.
     *
     * @return the rejected card; never {@code null}
     */
    public Card getAttemptedCard() {
        return attemptedCard;
    }

    /**
     * Returns the table sum at the time the illegal play was attempted.
     *
     * @return table sum (may be negative if face cards were played early)
     */
    public int getTableSum() {
        return tableSum;
    }
}
