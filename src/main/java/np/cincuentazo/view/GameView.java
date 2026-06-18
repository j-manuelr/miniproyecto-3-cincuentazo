package np.cincuentazo.view;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import np.cincuentazo.model.Card;
import np.cincuentazo.model.Player;

import java.util.List;

public class GameView {

    public static void renderTableCard(StackPane tablePile, Label lblLastCard, Card topCard) {
        lblLastCard.setText(topCard != null ? topCard.toString() : "-");

        if (tablePile.getChildren().size() > 1) {
            tablePile.getChildren().remove(1, tablePile.getChildren().size());
        }
        if (topCard != null) {
            CardView cv = new CardView(topCard, false);
            cv.setPrefSize(90, 128);
            cv.setMaxSize(90, 128);
            tablePile.getChildren().add(cv);
        }
    }

    public static void renderHumanHand(HBox humanHand, List<Card> cards, boolean isHumanTurn, int tableSum, java.util.function.IntFunction<EventHandler<MouseEvent>> clickHandlerFactory) {
        humanHand.getChildren().clear();
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            CardView cv = new CardView(card, false);
            boolean ok = card.isPlayable(tableSum);

            if (!isHumanTurn || !ok) cv.setCardDisabled(true);

            if (isHumanTurn && ok) {
                UiAnimations.applyCardMotion(cv);
                cv.setOnMouseClicked(clickHandlerFactory.apply(i));
            }

            humanHand.getChildren().add(cv);
        }
    }

    public static void renderMachineHand(HBox handBox, Label countLabel, int handSize, java.util.function.Function<Integer, Card> cardProvider) {
        handBox.getChildren().clear();
        countLabel.setText(String.valueOf(handSize));

        for (int i = 0; i < handSize; i++) {
            Card card = cardProvider.apply(i);
            handBox.getChildren().add(new CardView(card, true));
        }
    }

    public static void updateStatusBadge(Label label, Player player, Player current) {
        label.getStyleClass().removeAll("badge-active", "badge-waiting", "badge-eliminated");
        if (!player.isAlive()) {
            label.setText("Eliminado");
            label.getStyleClass().add("badge-eliminated");
        } else if (player == current) {
            label.setText("Turno");
            label.getStyleClass().add("badge-active");
        } else {
            label.setText("Esperando");
            label.getStyleClass().add("badge-waiting");
        }
    }

    public static void addEliminatedLogEntry(VBox elimList, String playerName) {
        Label entry = new Label("✘  " + playerName);
        entry.getStyleClass().add("elim-entry");
        elimList.getChildren().add(entry);
    }

}
