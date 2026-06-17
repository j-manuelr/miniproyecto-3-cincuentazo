package np.cincuentazo.model;

import np.cincuentazo.exception.InvalidPlayException;
import np.cincuentazo.interfaces.Playable;

import java.util.List;

/**
 * Abstract base class for all Cincuentazo players.
 * Implements {@link Playable} to enforce the contract for card selection.
 * Both {@code HumanPlayer} and {@code MachinePlayer} extend this class.
 */
public abstract class Player implements Playable {

    private final String name;
    private final Hand hand;
    private boolean alive;

    /**
     * Constructs a player with the given name.
     * The hand starts empty and the player is alive.
     *
     * @param name the player's display name
     */
    protected Player(String name) {
        this.name = name;
        this.hand = new Hand();
        this.alive = true;
    }

    /**
     * Selects and returns a card from the player's hand.
     * Concrete implementations define the selection strategy
     * (human: UI-driven; machine: automatic).
     *
     * @param cardIndex zero-based index of the card to play
     * @param tableSum  the current table sum
     * @return the played card (removed from hand)
     * @throws InvalidPlayException if the card would cause the sum to exceed 50
     */
    @Override
    public abstract Card selectCard(int cardIndex, int tableSum) throws InvalidPlayException;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canPlay(int tableSum) {
        return hand.canPlay(tableSum);
    }

    /**
     * Adds a card to this player's hand (used when drawing from the deck).
     *
     * @param card the card to add
     */
    public void drawCard(Card card) {
        hand.addCard(card);
    }

    /**
     * Marks this player as eliminated and returns all cards from their hand
     * so they can be recycled back into the deck.
     *
     * @return the list of cards surrendered by this player
     */
    public List<Card> eliminate() {
        alive = false;
        return hand.removeAll();
    }

    /**
     * Returns this player's display name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns this player's current hand.
     *
     * @return the hand
     */
    public Hand getHand() {
        return hand;
    }

    /**
     * Returns whether this player is still in the game.
     *
     * @return {@code true} if the player has not been eliminated
     */
    public boolean isAlive() {
        return alive;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
               "[" + name + ", alive=" + alive + ", " + hand + "]";
    }
}
