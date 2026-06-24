package np.cincuentazo.model;

import java.util.*;

/**
 * Holds the complete, authoritative state of a Cincuentazo game session.
 *
 * <h2>Single source of truth</h2>
 * <p>Every piece of information the controller or the view needs about the
 * ongoing game lives here: the deck, the table cards, the table sum, the
 * player list, and whose turn it is. Neither the controller nor the view
 * store game data independently — they always read from and write to this
 * object.</p>
 *
 * <h2>Separation of concerns</h2>
 * <p>{@code GameState} is a data model, not a rules engine. It exposes
 * mutation methods ({@link #playCard}, {@link #advanceTurn}) but enforces
 * no game rules internally. Rule enforcement (e.g., "is this card legal?",
 * "is the game over?") is the responsibility of the controller, which reads
 * state, checks rules, then calls the appropriate mutation.</p>
 *
 * <h2>Table cards structure</h2>
 * <p>A {@link Stack} is used for table cards because the game always interacts
 * with the <em>top</em> card: the last played card is visible on top, and when
 * recycling cards back to the deck, the top card stays while everything below
 * goes to the deck. Stack's {@code peek()} and {@code pop()} map naturally to
 * these operations.</p>
 */

public class GameState {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** The draw pile. Cards are dealt from here and returned here. */
    private final Deck deck;

    /**
     * Ordered pile of cards played to the table.
     * Bottom = first card played (the initial random card).
     * Top    = most recently played card.
     */
    private final Stack<Card> tableCards;

    /**
     * Running sum of all cards played to the table.
     * Must never exceed {@link Card#MAX_TABLE_SUM} (50).
     * Starts at the value of the first card placed at setup.
     */
    private int tableSum;

    /**
     * All players in turn order. Index 0 is always the human player.
     * Eliminated players remain in the list but have {@code alive = false}.
     */
    private final List<Player> players;

    /**
     * Index into {@link #players} pointing to the player whose turn it is.
     * Advanced by {@link #advanceTurn()} after each completed turn.
     */
    private int currentTurnIndex;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new game state for the given player list, using a freshly
     * shuffled deck. The first card is dealt to the table to start the sum.
     *
     * <h3>Setup sequence</h3>
     * <ol>
     *   <li>Store players (human at index 0, machines at 1-3).</li>
     *   <li>Deal 4 cards to each player (face up for human, face down for machines).</li>
     *   <li>Draw one card and place it face up on the table to seed the sum.</li>
     *   <li>Set {@code currentTurnIndex = 0} so the human goes first.</li>
     * </ol>
     *
     * @param players ordered list of players; index 0 must be the human player;
     *                must have 2–4 elements
     * @throws NullPointerException     if {@code players} is {@code null}
     * @throws IllegalArgumentException if the player count is outside [2, 4]
     */
    public GameState(List<Player> players){
        Objects.requireNonNull(players, "Player list must not be null.");
        if(players.size() < 2 || players.size() > 4){
            throw new IllegalArgumentException(
                    "Cincuentazo game requires 2 to 4 players, got: "+ players.size()
            );
        }

        this.players = new ArrayList<>(players);  // defensive copy (cambio 5)
        this.deck = new Deck();
        this.tableCards = new Stack<>();
        this.currentTurnIndex = 0;

        dealInitialHands();
        placeInitialTableCard();
    }

    // -------------------------------------------------------------------------
    // Setup helpers  (called only from constructor)
    // -------------------------------------------------------------------------

    /**
     * Deals 4 cards to each player.
     * Human player's cards are flipped face up; machine cards stay face down.
     */
    private void dealInitialHands(){
        for(int i = 0; i < Hand.HAND_SIZE; i++){
            for (int p = 0; p < players.size(); p++){
                Card card = deck.drawCard();
                if(p == 0){
                    // Human player sees their own cards
                    card.setFaceUp(true);
                }
                players.get(p).drawCard(card);
            }
        }
    }

    /**
     * Draws the first card from the deck, flips it face up, and places it
     * on the table to initialise the table sum.
     *
     * <p>Per the rules, this card's value seeds the sum:
     * <ul>
     *   <li>9  → sum starts at 0</li>
     *   <li>A  → sum starts at 1 (worst case; Ace always uses 1 when sum is 0
     *             because 0+10=10 ≤ 50, so actually starts at 10 — handled
     *             by {@link Card#getValue(int)} with tableSum=0)</li>
     *   <li>J/Q/K → sum starts at -10</li>
     * </ul>
     * </p>
     */
    private void placeInitialTableCard(){
        Card first = deck.drawCard();
        first.setFaceUp(true);
        tableCards.push(first);

        // HU-2 / "Otras consideraciones": the automatically dealt starting card
        // is not chosen by any player, so an Ace must start the table sum at 1
        // (never 10). first.getValue(0) would otherwise return 10 for an Ace
        // because 0 + 10 <= 50, which contradicts the rule. Every other rank
        // is unaffected by the tableSum argument (9 -> 0, J/Q/K -> -10, etc.).
        tableSum = first.getRank().isAce() ? 1 : first.getValue(0);
    }

    //-------------------------------------------------------------------------
    // Turn management
    // -------------------------------------------------------------------------

    /**
     * Advances {@code currentTurnIndex} to the next <em>alive</em> player,
     * wrapping around the list circularly.
     *
     * <p>This method assumes the game is not over — the caller must check
     * {@link #isGameOver()} before calling this, otherwise it would loop
     * forever looking for a second alive player that does not exist.</p>
     */
    public void advanceTurn(){
        do{
            currentTurnIndex= (currentTurnIndex + 1) % players.size();
        }while (!players.get(currentTurnIndex).isAlive());
    }

    // -------------------------------------------------------------------------
    // Game actions  (called by the controller after validation)
    // -------------------------------------------------------------------------

    /**
     * Places a card on the table and updates the table sum.
     *
     * <p>The card is flipped face up (it becomes visible to all), pushed onto
     * the table stack, and its value is added to the running sum.</p>
     *
     * <p><strong>Pre-condition:</strong> the controller must have validated
     * that playing this card is legal before calling this method.</p>
     *
     * @param card the card just played by the current player; must not be {@code null}
     */
    public void playCard (Card card){
        Objects.requireNonNull(card, "Cannot play a null card.");
        card.setFaceUp(true);
        int delta = card.getValue(tableSum);
        tableSum += delta;
        tableCards.push(card);
    }

    /**
     * Checks whether the deck is empty and, if so, refills it from the
     * table cards (all except the top card).
     *
     * <p>This must be called <em>before</em> drawing a replacement card at
     * the end of a player's turn. If the deck is not empty, this is a no-op.</p>
     *
     * @return {@code true} if a refill was performed, {@code false} if the
     *         deck had cards and no refill was needed
     */
    public boolean refillDeckIfNeeded(){
        if(!deck.isEmpty()) return false;

        // Convert the stack to a list for the refill method.
        // tableCards is modified in-place: only the last card remains.
        List<Card> tableList = new ArrayList<>(tableCards);
        int recycled = deck.refillFromTableCards(tableList);

        // Rebuild the stack from the modified list (only the last card remains).
        tableCards.clear();
        tableCards.addAll(tableList);
        return recycled > 0;
    }

    /**
     * Draws one card from the deck and gives it to the specified player.
     * Call {@link #refillDeckIfNeeded()} first if the deck might be empty.
     *
     * @param player the player who receives the drawn card; must not be {@code null}
     */
    public void dealCardToPlayer(Player player){
        Objects.requireNonNull(player, "Player must not be null.");
        Card drawn = deck.drawCard();
        // Machine cards stay face down; human cards are revealed.
        if (player instanceof HumanPlayer){
            drawn.setFaceUp(true);
        }
        player.drawCard(drawn);
    }

    /**
     * Eliminates the current player: marks them as out, removes their hand
     * cards, and sends those cards to the bottom of the deck.
     *
     * <p>Called when the current player has no playable card on their turn.</p>
     */
    public void eliminatedCurrentPlayer(){
        Player current = getCurrentPlayer();
        List<Card> returnedCards = current.eliminate();
        deck.addCardsToBottom(returnedCards);
    }

    // -------------------------------------------------------------------------
    // Game-over detection
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if only one player remains alive — that player
     * is the winner.
     *
     * <p>The controller calls this after every turn resolution (either after
     * a successful play or after an elimination) to decide whether to continue
     * or end the game.</p>
     *
     * @return {@code true} when the game has a winner
     */
    public boolean isGameOver(){
        long aliveCount = players.stream().filter(Player::isAlive).count();
        return aliveCount <= 1;
    }

    /**
     * Returns the winning player, or {@code null} if the game is not over yet.
     *
     * @return the sole surviving player, or {@code null}
     */
    public Player getWinner(){
        if(!isGameOver()) return null;
        return players.stream().filter(Player::isAlive).findFirst().orElse(null);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * @return the player whose turn it currently is; never {@code null}
     */
    public Player getCurrentPlayer(){
        return players.get(currentTurnIndex);
    }

    /**
     * @return the current table sum (can be negative if J/Q/K were the first cards played)
     */
    public int getTableSum(){
        return tableSum;
    }

    /**
     * @return the card most recently played to the table (top of the stack),
     *         or {@code null} if no card has been played yet
     */
    public Card getTopTableCard(){
        return tableCards.isEmpty()? null : tableCards.peek();
    }

    /**
     * @return an unmodifiable view of all players, in turn order
     */
    public List<Player> getPlayers(){
        return Collections.unmodifiableList(players);
    }

    /**
     * @return the draw pile
     */
    public Deck getDeck(){
        return deck;
    }

    /**
     * @return the number of cards currently on the table
     */
    public int getTableCardsCount(){
        return tableCards.size();
    }

    /**
     * @return the 0-based index of the current player in the player list
     */
    public int getCurrentTurnIndex(){
        return currentTurnIndex;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Returns a compact summary of the game state for debugging.
     * Example: {@code "GameState[sum=34, turn=Tú, deck=28, table=7 cards]"}.
     */
    @Override
    public String toString(){
        return "GameState[sum =" + tableSum +
                " turn = " + getCurrentPlayer().getName() +
                ", " + deck +
                "table= " + tableCards.size() + " cards]";
    }
}
