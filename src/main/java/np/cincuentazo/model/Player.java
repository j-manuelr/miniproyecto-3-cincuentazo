package np.cincuentazo.model;

import java.util.List;

public abstract class Player {

    private final String name;
    private final Hand hand;
    private boolean alive;

    protected Player(String name){
        this.name = name;
        this.hand = new Hand();
        this.alive = true;
    }

    public abstract Card selectCard (int cardIndex, int tableSum)
            throws np.cincuentazo.exception.InvalidPlayException;

    public boolean canPlay(int tableSum){
        return hand.canPlay(tableSum);
    }

    public void drawCard(Card card){
        hand.addCard(card);
    }

    public List<Card> eliminate(){
        alive = false;
        return hand.removeAll();
    }

    public String getName(){
        return name;
    }

    public Hand getHand(){
        return hand;
    }

    public boolean isAlive(){
        return alive;
    }

    @Override
    public String toString(){
        return getClass().getSimpleName() +
                "[" + name + ", alive=" + alive + "," + hand + "]";
    }
}
