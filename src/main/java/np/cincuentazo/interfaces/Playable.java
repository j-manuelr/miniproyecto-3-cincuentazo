package np.cincuentazo.interfaces;

import np.cincuentazo.exception.InvalidPlayException;
import np.cincuentazo.model.Card;

/**
 * Defines the contract for any entity that can select and play a card
 * in the Cincuentazo game. Implemented by the abstract {@code Player} class
 * and therefore by both {@code HumanPlayer} and {@code MachinePlayer}.
 */
public interface Playable {

    /**
     * Selects the card at the given index from the player's hand and returns it.
     * The card is removed from the hand if the selection is valid.
     *
     * @param cardIndex zero-based index of the chosen card in the hand
     * @param tableSum  the current table sum before the card is played
     * @return the selected card
     * @throws InvalidPlayException if the selected card would cause the sum to exceed 50
     */
    Card selectCard(int cardIndex, int tableSum) throws InvalidPlayException;

    /**
     * Returns whether this player has at least one card that can be played
     * without the table sum exceeding 50.
     *
     * @param tableSum the current table sum
     * @return {@code true} if a valid play exists
     */
    boolean canPlay(int tableSum);
}
