package np.cincuentazo.model;

import java.util.Objects;

public class Card {

    public static final int MAX_TABLE_SUM= 50;

    private final Suit suit;
    private final Rank rank;
    private boolean faceUp;

    public Card (Suit suit, Rank rank){
        this.suit = suit;
        this.rank = rank;
        faceUp = false;
    }

    public int getValue(int tableSum){
        if(rank.isAce()){
            return  (tableSum + 10 <=MAX_TABLE_SUM) ? 10 : 1;
        }
        return rank.getBaseValue();
    }

    public boolean isPlayable(int tableSum){
        return tableSum + getValue(tableSum) <= MAX_TABLE_SUM;
    }

    public void flip(){
        faceUp = !faceUp;
    }

    public void setFaceUp(boolean faceUp){
        this.faceUp = faceUp;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof  Card other)) return false;

        return suit == other.suit && rank == other.rank;
    }

    @Override
    public int hashCode(){
        return Objects.hash(suit,rank);
    }

    @Override
    public String toString(){
        return  rank.getLabel() + suit.getSymbol();

    }
}
