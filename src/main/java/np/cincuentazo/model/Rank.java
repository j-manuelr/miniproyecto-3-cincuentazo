package np.cincuentazo.model;
/**
 * Represents the thirteen ranks of a standard poker deck, each carrying
 * the scoring logic defined by the Cincuentazo rules.
 *
 * <h2>Value rules</h2>
 * <ul>
 *   <li>2–8, 10 → add their face number to the table sum.</li>
 *   <li>9        → neutral; neither adds nor subtracts (value = 0).</li>
 *   <li>J, Q, K  → subtract 10 from the table sum (value = -10).</li>
 *   <li>A        → adds 1 or 10, whichever keeps the sum ≤ 50.
 *                  This decision requires the current table sum, so the
 *                  Ace's final value is resolved in {@link Card#getValue(int)}
 *                  rather than here. {@code getBaseValue()} returns
 *                  {@link #ACE_FLEXIBLE} as a sentinel for that case.</li>
 * </ul>
 *
 * <h2>Why put value logic in the enum?</h2>
 * <p>The value of a card depends exclusively on its rank — not on which
 * specific card object is in play. Encoding it here avoids scattering
 * {@code switch} or {@code if-else} chains across the codebase and makes
 * it trivial to unit-test every rank in isolation.</p>
 */

public enum Rank {

    // Constants  (name, display label, base value)

    TWO ("2", 2),
    THREE ("3", 3),
    FOUR ("4", 4),
    FIVE ("5", 5),
    SIX ("6", 6),
    SEVEN ("7", 7),
    EIGHT ("8", 8),
    NINE ("9", 0),
    TEN ("10", 10),
    JACK ("J", -10),
    QUEEN ("Q", -10),
    KING ("K", -10),
    ACE ("A", Constants.ACE_FLEXIBLE); // resolved dynamically

    /**
     * Inner static class used to hold the sentinel constant BEFORE the enum
     * constants are initialized. This pattern avoids the "illegal forward
     * reference" compile error that occurs when a {@code static final} field
     * is declared after the enum constants that reference it.
     *
     * <p>Java initializes static inner classes before the enclosing enum's
     * own static members, so {@code Constants.ACE_FLEXIBLE} is always
     * available when the {@code ACE} constant is being constructed.</p>
     */
    private static final class Constants{
        /**
         * Sentinel value used as the base value for the Ace.
         * Any caller that sees this must delegate to {@link Card#getValue(int)}.
         * {@code Integer.MIN_VALUE} ensures accidental arithmetic produces
         * obviously wrong results, making the bug visible immediately.
         */
        static  final int ACE_FLEXIBLE = Integer.MIN_VALUE;
    }
    /**
     * Public alias so external classes can reference the sentinel without
     * knowing about the inner {@code Constants} class.
     */
    public static final int ACE_FLEXIBLE = Constants.ACE_FLEXIBLE;

    /** Short label shown on the card face (e.g., "J", "10", "A"). */
    private final String label;

    /**
     * Pre-computed scoring value for all ranks except the Ace.
     * For the Ace, this field holds {@link #ACE_FLEXIBLE} as a sentinel.
     */
    private final int baseValue;

    //Constructor
    /**
     * @param label     display text for this rank
     * @param baseValue scoring value, or {@link #ACE_FLEXIBLE} for the Ace
     */
    Rank(String label, int baseValue){
        this.label = label;
        this.baseValue = baseValue;
    }

    /**
     * Returns the short display label for this rank.
     * Examples: {@code "2"}, {@code "10"}, {@code "J"}, {@code "A"}.
     *
     * @return non-null, non-empty label string
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the base scoring value for this rank.
     *
     * <p><strong>Important:</strong> for {@link #ACE}, this method returns
     * {@link #ACE_FLEXIBLE}. Callers must not use that value in arithmetic;
     * they must call {@link Card#getValue(int)} instead to obtain the
     * context-aware value (1 or 10).</p>
     *
     * @return the scoring delta this rank applies to the table sum,
     *         or {@link #ACE_FLEXIBLE} if the rank is ACE
     */
    public int getBaseValue() {
        return baseValue;
    }

    /**
     * Convenience predicate — returns {@code true} if this rank is an Ace
     * and therefore requires a context-dependent value resolution.
     *
     * @return {@code true} only for {@link #ACE}
     */
    public boolean isAce(){
        return  this == ACE;
    }

    /**
     * Returns a human-readable string combining the label and its base value.
     * Example: {@code "J(-10)"}, {@code "9(0)"}, {@code "A(flexible)"}.
     *
     * @return formatted string for debugging and logging
     */
    @Override
    public String toString() {
        if (isAce()) return label + "(flexible)";
        return label + "(" + baseValue + ")";
    }
}

