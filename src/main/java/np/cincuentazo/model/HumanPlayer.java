package np.cincuentazo.model;

import np.cincuentazo.exception.InvalidPlayException;

public class HumanPlayer extends Player{
    public HumanPlayer(String name){
        super(name);
    }

    @Override
    public Card selectCard(int cardIndex, int tableSum) throws InvalidPlayException {
        return getHand().removeCard(cardIndex, tableSum);
    }
}
