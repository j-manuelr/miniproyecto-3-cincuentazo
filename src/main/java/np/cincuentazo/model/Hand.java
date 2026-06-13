package np.cincuentazo.model;

import np.cincuentazo.exception.InvalidPlayException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hand {
    public static final int HAND_SIZE = 4;

    private final List<Card> cards;

    public Hand(){
        cards = new ArrayList<>(HAND_SIZE);
    }

    public void addCard(Card card){
        if(card == null) throw new NullPointerException("Cannot add a null card to the hand.");
        cards.add(card);
    }
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

    public List<Card> removeAll(){
        List<Card> removed = new ArrayList<>(cards);
        cards.clear();

        return removed;
    }

    public List<Card> getPlayableCards(int tableSum){
        List<Card> playable = new ArrayList<>();
        for (Card c : cards){
            if (c.isPlayable(tableSum)){
              playable.add(c);
            }
        }
        return playable;
    }

    public boolean canPlay (int tableSum){
        for (Card c : cards){
            if (c.isPlayable(tableSum)) return true;
        }
        return false;
    }

    public List<Card> getCards(){
        return Collections.unmodifiableList(cards);
    }

    public Card getCard(int index){
        return cards.get(index);
    }

    public int getHandSize(){
        return cards.size();
    }

    public boolean isEmpty(){
        return cards.isEmpty();
    }

    @Override
    public String toString(){
        return "Hand: " + cards.toString();
    }
}
