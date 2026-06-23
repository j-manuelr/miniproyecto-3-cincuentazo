package np.cincuentazo.model;

import np.cincuentazo.exception.InvalidPlayException;
/**
 * Represents the human player in a Cincuentazo game.
 *
 * <h2>Role in MVC</h2>
 * <p>This class is pure model: it holds no reference to any UI component.
 * The controller is the bridge between UI events (mouse clicks on card slots)
 * and this class's {@link #selectCard(int, int)} method.</p>
 *
 * <h2>How a human turn works</h2>
 * <ol>
 *   <li>The controller enables card-click events on the UI.</li>
 *   <li>The player clicks a card — the view fires an event with the card's index.</li>
 *   <li>The controller receives the event and calls {@code selectCard(index, tableSum)}.</li>
 *   <li>If the play is valid, the card is returned; if not, {@link InvalidPlayException}
 *       is thrown and the UI can display an error without crashing.</li>
 * </ol>
 */
public class HumanPlayer extends Player{
    //Constructor
    /**
     * Creates the human player.
     *
     * @param name the display name (e.g., {@code "Tú"})
     */
    public HumanPlayer(String name){
        super(name);
    }

    //Abstract method implementation
    /**
     * Plays the card at the given index on behalf of the human player.
     *
     * <p>This method is called by the controller <em>after</em> the human
     * has clicked a card in the UI. The controller passes the clicked card's
     * index and the current table sum for validation.</p>
     *
     * <p>The method delegates directly to {@link Hand#removeCard(int, int)},
     * which performs the legality check and extracts the card atomically.</p>
     *
     * @param cardIndex 0-based index of the clicked card in the hand
     * @param tableSum  current table sum at the moment of play
     * @return the card that was played; never {@code null}
     * @throws InvalidPlayException if playing the card would make the table
     *         sum exceed 50 — the controller should show a warning in the UI
     *         and keep the card in the hand
     */
    @Override
    public Card selectCard(int cardIndex, int tableSum) throws InvalidPlayException {
        return getHand().removeCard(cardIndex, tableSum);
    }
}
