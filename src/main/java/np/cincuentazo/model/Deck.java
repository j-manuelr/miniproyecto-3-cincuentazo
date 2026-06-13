package np.cincuentazo.model;

import np.cincuentazo.exception.EmptyDeckException;

import java.util.ArrayList;
import  java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class Deck {

    private final LinkedList<Card> cards;

    public Deck(){
        cards = new LinkedList<>(buildFullDeck());
        shuffle();
    }

    Deck(LinkedList<Card> cards){
        this.cards = new LinkedList<>(cards);
    }

    private List<Card> buildFullDeck(){
        List<Card> deck = new ArrayList<>(52);
        for (Suit suit: Suit.values()){
            for (Rank rank: Rank.values()){
                deck.add(new Card(suit, rank));
            }
        }
        return  deck;
    }

    public  void shuffle(){
        Collections.shuffle(cards);
    }

    public Card drawCard(){
//        if(cards.isEmpty()){
//            throw new EmptyDeckException(
//                    "Attempted to draw an empty deck. " +
//                            "Call refillFromTableCards() before drawing."
//            );
//        }
       return cards.pollFirst();
    }

    public void addCardsToBottom(List<Card> newCards){
        if(newCards == null || newCards.isEmpty()) return;
        for (Card c : newCards){
            drawCard().setFaceUp(false);
            cards.addLast(c);
        }
    }

    public int refillFromTableCards(List<Card> tableCards){
        if(tableCards == null || tableCards.isEmpty()){
            throw new IllegalArgumentException(
                    "Cannot refill deck: tableCards is null or empty."
            );
        }
        List<Card> toRecycle = new ArrayList<>(
                tableCards.subList(0, tableCards.size() - 1)
        );

        tableCards.removeAll(toRecycle);

        Collections.shuffle(toRecycle);
        addCardsToBottom(toRecycle);

        return toRecycle.size();
    }

    public boolean isEmpty(){
        return cards.isEmpty();
    }

    public int size(){
        return cards.size();
    }

    @Override
    public String toString(){
        return  "Deck [remaining = " + cards.size() + "]";
    }

}
