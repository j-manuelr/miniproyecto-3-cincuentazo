package np.cincuentazo.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import np.cincuentazo.model.Card;
import np.cincuentazo.model.Suit;

/**
 * JavaFX component that renders a single playing card.
 *
 * <p>Face-up cards show the rank, suit symbol, and a value hint.
 * Face-down cards show the card back graphic.
 * Supports {@code selected} and {@code disabled} CSS states for interactive feedback.
 */
public class CardView extends StackPane {

    private final Card card;
    private boolean selected = false;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a card view for the given card.
     *
     * @param card          the card to display; may be {@code null} for placeholder backs
     * @param isMachineCard {@code true} to apply the smaller machine-card CSS dimensions
     */
    public CardView(Card card, boolean isMachineCard) {
        this.card = card;

        if (card != null && card.isFaceUp()) {
            buildFaceUpView(isMachineCard);
        } else {
            buildFaceDownView(isMachineCard);
        }
    }

    // -------------------------------------------------------------------------
    // Face-up / face-down builders
    // -------------------------------------------------------------------------

    private void buildFaceUpView(boolean isMachineCard) {
        getStyleClass().add("card");
        if (isMachineCard) {
            setMinSize(56, 80);
            setMaxSize(56, 80);
        }

        boolean isRed = card.getSuit() == Suit.HEARTS || card.getSuit() == Suit.DIAMONDS;
        String colorClass = isRed ? "card-red" : "card-black";

        // Top-left: rank + suit symbol
        Label rankLabel = new Label(card.getRank().getLabel() + " " + card.getSuit().getSymbol());
        rankLabel.getStyleClass().addAll("card-rank", colorClass);

        // Centre: large suit symbol
        Label suitLabel = new Label(card.getSuit().getSymbol());
        suitLabel.getStyleClass().addAll("card-suit-center", colorClass);

        // Bottom: value hint
        Label hintLabel = new Label(buildValueHint());
        hintLabel.getStyleClass().add("card-value-hint");

        VBox layout = new VBox(2, rankLabel, suitLabel, hintLabel);
        layout.setAlignment(Pos.CENTER);
        getChildren().add(layout);
    }

    private void buildFaceDownView(boolean isMachineCard) {
        if (isMachineCard) {
            getStyleClass().addAll("card-back", "machine-card");
        } else {
            getStyleClass().add("card-back");
        }
    }

    // -------------------------------------------------------------------------
    // Visual state helpers
    // -------------------------------------------------------------------------

    /**
     * Toggles the selected (raised) visual state of this card.
     *
     * @param selected {@code true} to mark the card as selected
     */
    public void setCardSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            if (!getStyleClass().contains("card-selected"))
                getStyleClass().add("card-selected");
        } else {
            getStyleClass().remove("card-selected");
        }
    }

    /**
     * Toggles the disabled (greyed-out) state for cards that cannot be played.
     *
     * @param disabled {@code true} to mark the card as unplayable
     */
    public void setCardDisabled(boolean disabled) {
        if (disabled) {
            if (!getStyleClass().contains("card-disabled"))
                getStyleClass().add("card-disabled");
        } else {
            getStyleClass().remove("card-disabled");
        }
    }

    /**
     * Returns the model card associated with this view.
     *
     * @return the card, or {@code null} for placeholder views
     */
    public Card getCard() {
        return card;
    }

    /** Returns whether this card view is in the selected state. */
    public boolean isCardSelected() {
        return selected;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildValueHint() {
        int base = card.getRank().getBaseValue();
        if (card.getRank().isAce()) return "+1 / +10";
        if (base == 0)  return "±0";
        if (base > 0)   return "+" + base;
        return String.valueOf(base); // negative already carries the minus sign
    }
}
