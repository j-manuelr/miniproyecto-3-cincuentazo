package np.cincuentazo.model;

import java.util.*;

public class GameState {
    private final Deck deck;
    private final Stack<Card> tableCards;
    private int tableSum;
    private final List<Player> players;
    private int currentTurnIndex;

    public GameState(List<Player> players){
        Objects.requireNonNull(players, "Player list must not be null.");
        if(players.size() < 2 || players.size() > 4){
            throw new IllegalArgumentException(
                    "Cincuentazo game requires 2 to 4 players, got: "+ players.size()
            );
        }

        this.players = players;
        this.deck = new Deck();
        this.tableCards = new Stack<>();
        this.currentTurnIndex = 0;

        dealInitialHands();
        placeInitialTableCard();
    }

    private void dealInitialHands(){
        for(int i = 0; i < Hand.HAND_SIZE; i++){
            for (int p = 0; p < players.size(); p++){
                Card card = deck.drawCard();
                if(p == 0){
                    card.setFaceUp(true);
                }
                players.get(p).drawCard(card);
            }
        }
    }

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

    public void advanceTurn(){
        do{
            currentTurnIndex= (currentTurnIndex + 1) % players.size();
        }while (!players.get(currentTurnIndex).isAlive());
    }

    public void playCard (Card card){
        Objects.requireNonNull(card, "Cannot play a null card.");
        card.setFaceUp(true);
        int delta = card.getValue(tableSum);
        tableSum += delta;
        tableCards.push(card);
    }

    public boolean refillDeckIfNeeded(){
        if(!deck.isEmpty()) return false;

        List<Card> tableList = new ArrayList<>(tableCards);
        int recycled = deck.refillFromTableCards(tableList);
        tableCards.clear();
        tableCards.addAll(tableList);

        return recycled > 0;
    }

    public void dealCardToPlayer(Player player){
        Objects.requireNonNull(player, "Player must not be null.");
        Card drawn = deck.drawCard();

        if (player instanceof HumanPlayer){
            drawn.setFaceUp(true);
        }
        player.drawCard(drawn);
    }

    public void eliminatedCurrentPlayer(){
        Player current = getCurrentPlayer();
        List<Card> returnedCards = current.eliminate();
        deck.addCardsToBottom(returnedCards);
    }

    public boolean isGameOver(){
        long aliveCount = players.stream().filter(Player::isAlive).count();
        return aliveCount <= 1;
    }

    public Player getWinner(){
        if(!isGameOver()) return null;
        return players.stream().filter(Player::isAlive).findFirst().orElse(null);
    }

    public Player getCurrentPlayer(){
        return players.get(currentTurnIndex);
    }
    public int getTableSum(){
        return tableSum;
    }

    public Card getTopTableCard(){
        return tableCards.isEmpty()? null : tableCards.peek();
    }

    public List<Player> getPlayers(){
        return Collections.unmodifiableList(players);
    }

    public Deck getDeck(){
        return deck;
    }

    public int getTableCardsCount(){
        return tableCards.size();
    }

    public int getCurrentTurnIndex(){
        return currentTurnIndex;
    }

    @Override
    public String toString(){
        return "GameState[sum =" + tableSum +
                " turn = " + getCurrentPlayer().getName() +
                ", " + deck +
                "table= " + tableCards.size() + " cards]";
    }
}
