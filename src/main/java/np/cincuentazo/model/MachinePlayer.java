package np.cincuentazo.model;

import np.cincuentazo.exception.InvalidPlayException;

import java.util.Comparator;
import java.util.List;
/**
 * Represents an AI-controlled player in Cincuentazo.
 *
 * <h2>Strategy</h2>
 * <p>The machine uses an <em>aggressive strategy</em>: among all legally
 * playable cards, it always picks the one that results in the <em>highest</em>
 * table sum (without exceeding 50). This maximises the pressure on the
 * next player by leaving them as little room as possible.</p>
 *
 * <p>This is intentionally kept simple for a first implementation. A more
 * advanced strategy could consider the probability of the next player being
 * able to respond, or weigh saving J/Q/K cards for high-sum situations.</p>
 *
 * <h2>Thread safety note</h2>
 * <p>{@link #chooseBestCardIndex(int)} is called from a background thread
 * ({@code MachinePlayerThread}) and must therefore operate only on
 * <em>model state</em> — no JavaFX calls, no shared mutable state beyond
 * the hand (which is not modified here). The actual {@link #selectCard}
 * call happens on the JavaFX Application Thread, coordinated by the
 * controller via {@code Platform.runLater()}.</p>
 */

public class MachinePlayer extends Player{

    // Constructor

    /**
     * Creates a machine player.
     *
     * @param name display name shown in the UI (e.g., {@code "Máquina 1"})
     */
    public MachinePlayer(String name){
        super(name);
    }

    // Abstract method implementation

    /**
     * Plays the card at the given index on behalf of the machine.
     *
     * <p>This method is called by the controller on the JavaFX Application
     * Thread <em>after</em> the background thread has already computed
     * the best card index via {@link #chooseBestCardIndex(int)}.</p>
     *
     * @param cardIndex the index previously returned by {@link #chooseBestCardIndex}
     * @param tableSum  current table sum at the moment of play
     * @return the card that was played; never {@code null}
     * @throws InvalidPlayException should not occur in practice, because the
     *         index comes from {@link #chooseBestCardIndex}, which only
     *         returns indices of legally playable cards; present in the
     *         signature to satisfy the contract and for defensive safety
     */
    @Override
    public Card selectCard(int cardIndex, int tableSum) throws InvalidPlayException {
        return getHand().removeCard(cardIndex, tableSum);
    }

    // AI logic  (safe to call from a background thread — read-only)

    /**
     * Determines the index of the best card to play from the machine's hand,
     * given the current table sum.
     *
     * <h3>Algorithm</h3>
     * <ol>
     *   <li>Filter the hand for legally playable cards.</li>
     *   <li>Among playable cards, pick the one that maximises
     *       {@code tableSum + card.getValue(tableSum)} — the highest resulting
     *       table sum without busting.</li>
     *   <li>Return the index of that card in the full hand list.</li>
     * </ol>
     *
     * <p>This method is <em>read-only</em>: it does not modify the hand.
     * It is safe to call from any thread.</p>
     *
     * @param tableSum the current table sum
     * @return the 0-based index in the hand of the best card to play
     * @throws IllegalStateException if the machine has no playable cards —
     *         the caller ({@code MachinePlayerThread}) should have checked
     *         {@link Player#canPlay(int)} first
     */

    public int chooseBestCardIndex(int tableSum){
        List<Card> playable = getHand().getPlayableCards(tableSum);
        if (playable.isEmpty()){
            throw new IllegalStateException(
                    getName() + " has no playable cards. Check canPlay() before calling chooseBestCardIndex()."
            );
        }

        // Select the card that leaves the table sum as high as possible
        // — maximises pressure on the next player.
        Card best = playable.stream().max(Comparator.comparing(c -> tableSum + c.getValue(tableSum))).orElseThrow();
        // Translate back to the index in the full hand (not just the playable subset).
        return getHand().getCards().indexOf(best);
    }
}
