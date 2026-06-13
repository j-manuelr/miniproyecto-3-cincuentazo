package np.cincuentazo.model;

import np.cincuentazo.exception.InvalidPlayException;

import java.util.Comparator;
import java.util.List;

public class MachinePlayer extends Player{

    public MachinePlayer(String name){
        super(name);
    }

    @Override
    public Card selectCard(int cardIndex, int tableSum) throws InvalidPlayException {
        return getHand().removeCard(cardIndex, tableSum);
    }

    public int ChooseBestCardIndex(int tableSum){
        List<Card> playable = getHand().getPlayableCards(tableSum);
        if (playable.isEmpty()){
            throw new IllegalStateException(
                    getName() + "has no playable cards. Check canPlay() before calling chooseBestCardIndex()."
            );
        }

        Card best = playable.stream().max(Comparator.comparing(c -> tableSum + c.getValue(tableSum))).orElseThrow();
        // Translate back to the index in the full hand (not just the playable subset).
        return getHand().getCards().indexOf(best)
    }
}
