package np.cincuentazo.exception;

import np.cincuentazo.model.Card;

public class InvalidPlayException extends Exception{
    private final Card attempetedCard;

    private final int tableSum;

    public InvalidPlayException(Card attempetedCard, int tableSum){
        super(buildMessage(attempetedCard, tableSum));
        this.attempetedCard = attempetedCard;
        this.tableSum      = tableSum;
    }

    private static String buildMessage(Card card, int sum) {
        int resultingSum = sum + card.getValue(sum);
        return "Cannot play " + card + ": table sum would become " +
                resultingSum + " (max allowed is " + Card.MAX_TABLE_SUM + "). " +
                "Current sum: " + sum + ".";
    }

    /** @return the card that triggered this exception */
    public Card getAttemptedCard() {
        return attempetedCard;
    }

    /** @return the table sum at the time the illegal play was attempted */
    public int getTableSum() {
        return tableSum;
    }
}


