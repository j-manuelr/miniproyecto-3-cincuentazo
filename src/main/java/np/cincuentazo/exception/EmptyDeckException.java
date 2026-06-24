package np.cincuentazo.exception;

/**
 * Thrown when an attempt is made to draw a card from an empty {@link np.cincuentazo.model.Deck}.
 *
 * <p>This is an unchecked exception ({@link RuntimeException}) because an
 * empty deck is a programming error — the caller is always responsible for
 * calling {@link np.cincuentazo.model.GameState#refillDeckIfNeeded()} before
 * drawing, so an empty deck should never be reached under correct game flow.
 * Forcing every draw-site to handle a checked exception would be noise that
 * obscures real logic errors.</p>
 *
 * <p>Two constructors are provided to match the two standard
 * {@link RuntimeException} signatures: one that accepts only a message (for
 * direct throws) and one that also wraps a cause (for rethrowing caught
 * exceptions).</p>
 */
public class EmptyDeckException extends RuntimeException {


    public EmptyDeckException(String message) {
        super(message);
    }

    public EmptyDeckException(String message, Throwable cause) {
        super(message, cause);
    }
}