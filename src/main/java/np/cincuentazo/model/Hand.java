package np.cincuentazo.model;

import np.cincuentazo.exception.InvalidPlayException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the hand of cards held by a single player during the game.
 *
 * <h2>Invariant</h2>
 * <p>During active gameplay, a hand contains exactly 4 cards. This invariant
 * is enforced by the game flow (the controller deals 4 cards at setup and
 * ensures the player draws one replacement after every play), but it is
 * <em>not</em> enforced by this class itself — the Hand is a pure data
 * structure without game-rule knowledge.</p>
 *
 * <h2>Index stability</h2>
 * <p>Cards are stored in an {@link ArrayList} so each card has a stable
 * 0-based index. The UI maps visual card slots to these indices, so removing
 * a card at index {@code i} shifts subsequent cards — the controller must
 * re-render the hand after every removal.</p>
 */
public class Hand {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** The expected number of cards in a hand during normal gameplay. */

    public static final int HAND_SIZE = 4;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    /** The cards currently in this hand, in insertion order. */
    private final List<Card> cards;
// -------------------------------------------------------------------------
// Constructor
// -------------------------------------------------------------------------

/**
 * Creates an empty hand. Cards are added via {@link #addCard(Card)}
 * as they are dealt during setup.
 */
    public Hand(){
        cards = new ArrayList<>(HAND_SIZE);
    }

    // -------------------------------------------------------------------------
    // Mutation
    // -------------------------------------------------------------------------

    /**
     * Adds a card to the end of the hand.
     *
     * <p>Called in two situations:
     * <ol>
     *   <li>During initial deal (4 times per player).</li>
     *   <li>After a player plays a card — they draw one replacement so the
     *       hand returns to the expected size.</li>
     * </ol>
     *
     *
     * @param card the card to add; must not be {@code null}
     * @throws NullPointerException if {@code card} is {@code null}
     */

    public void addCard(Card card){
        if(card == null) throw new NullPointerException("Cannot add a null card to the hand.");
        cards.add(card);
    }

    /**
     * Removes and returns the card at the given index.
     *
     * <p>This is the primary play action: the player selects a card by its
     * visual slot (index), the controller calls this method to retrieve it,
     * and then places it on the table.</p>
     *
     * @param index 0-based position in the hand
     * @return the removed card
     * @throws IndexOutOfBoundsException if {@code index} is negative or
     *         ≥ {@link #getHandSize()}
     * @throws InvalidPlayException      if the card at {@code index} cannot
     *         legally be played given the current {@code tableSum}
     *         (checked variant so the controller must handle it explicitly)
     */
    public Card removeCard(int index, int tableSum) throws InvalidPlayException{
        if(index < 0 || index>= cards.size()){
            throw new IndexOutOfBoundsException(
                    "Hand index " + index + " out of bounds for hand size " + cards.size()
            );
        }
        Card card = cards.get(index);
        if (!card.isPlayable(tableSum)){
            throw new InvalidPlayException(card, tableSum);
        }
        return cards.remove(index);
    }

    /**
     * Removes all cards from the hand and returns them as a new list.
     *
     * <p>Called when a player is eliminated: all their remaining cards must
     * be sent to the bottom of the deck.</p>
     *
     * @return a new list containing all cards that were in the hand;
     *         the hand is empty after this call
     */

    public List<Card> removeAll(){
        List<Card> removed = new ArrayList<>(cards);
        cards.clear();

        return removed;
    }

    // -------------------------------------------------------------------------
    // Game-logic queries
    // -------------------------------------------------------------------------

    /**
     * Returns the subset of cards in this hand that can legally be played
     * given the current table sum.
     *
     * <p>A card is legally playable when {@code tableSum + card.getValue(tableSum) ≤ 50}.
     * If the returned list is empty, the player has no valid move and is
     * eliminated from the game.</p>
     *
     * @param tableSum the current sum on the table
     * @return a new, possibly empty, list of playable cards;
     *         order matches the hand's internal order
     */

    public List<Card> getPlayableCards(int tableSum){
        List<Card> playable = new ArrayList<>();
        for (Card c : cards){
            if (c.isPlayable(tableSum)){
              playable.add(c);
            }
        }
        return playable;
    }

    /**
     * Returns {@code true} if the player has at least one legally playable card.
     *
     * <p>This is the fast-path check used at the start of every turn.
     * If it returns {@code false}, the player is eliminated without needing
     * to build the full playable list.</p>
     *
     * @param tableSum the current sum on the table
     * @return {@code true} if {@link #getPlayableCards(int)} would return
     *         a non-empty list
     */
    public boolean canPlay (int tableSum){
        for (Card c : cards){
            if (c.isPlayable(tableSum)) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Read-only accessors
    // -------------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of the cards in this hand.
     *
     * <p>The view reflects the live state of the hand — if a card is later
     * added or removed, the view will reflect those changes. Callers must
     * not try to add or remove through the returned reference.</p>
     *
     * @return an unmodifiable list of cards in hand order
     */

    public List<Card> getCards(){
        return Collections.unmodifiableList(cards);
    }

    /**
     * Returns the card at the given index without removing it.
     * Useful for the UI to inspect a card before committing to play it.
     *
     * @param index 0-based position
     * @return the card at that position
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Card getCard(int index){
        return cards.get(index);
    }

    /**
     * Returns the number of cards currently in this hand.
     *
     * @return card count (0 to {@link #HAND_SIZE} under normal gameplay)
     */
    public int getHandSize(){
        return cards.size();
    }

    /**
     * @return {@code true} if the hand contains no cards
     */
    public boolean isEmpty(){
        return cards.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Returns a string listing all cards in the hand.
     * Example: {@code "Hand[A♥, 7♦, J♠, 3♣]"}.
     *
     * @return formatted string for debugging and logging
     */
    @Override
    public String toString(){
        return "Hand: " + cards.toString();
    }
}
