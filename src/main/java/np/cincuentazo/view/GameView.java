package np.cincuentazo.view;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import np.cincuentazo.model.Card;
import np.cincuentazo.model.Player;

import java.util.List;

/**
 * Pure-rendering utility class for the main game screen.
 *
 * <p>All methods are {@code static}: {@code GameView} owns no state of its
 * own and never mutates the model. Its single responsibility is to translate
 * a snapshot of model objects ({@link Card}, {@link Player}) into JavaFX
 * nodes, applying the correct CSS classes and event handlers. If how the
 * game <em>looks</em> changes, this is the only class that needs to change.</p>
 *
 * <h2>Why static methods instead of an instance?</h2>
 * <p>{@code GameController} is the FXML controller, so it is the only class
 * allowed to receive {@code @FXML}-injected nodes — JavaFX requires that.
 * Rather than wiring a separate instance and passing every node through a
 * constructor, static helpers accept the nodes as parameters on each call.
 * This avoids a 12-parameter constructor while keeping rendering logic out
 * of the controller.</p>
 *
 * <h2>SRP note</h2>
 * <p>Before this class was introduced, every rendering method lived inside
 * {@code GameController}, mixed with event handling, turn orchestration, and
 * scene navigation. Extracting these methods gives each class exactly one
 * reason to change.</p>
 */
public class GameView {

    /** Utility class — not instantiable. */
    private GameView() {}

    // =========================================================================
    // Table area
    // =========================================================================

    /**
     * Updates the table pile display and the last-card text label.
     *
     * <p>Removes all card views previously added to {@code tablePile} (keeping
     * the background {@link javafx.scene.layout.Pane} at index 0 intact), then
     * adds a new {@link CardView} for {@code topCard} if one exists.</p>
     *
     * @param tablePile   the {@link StackPane} that renders the top of the
     *                    table stack; must have at least one child (the background)
     * @param lblLastCard label that shows a text description of the top card
     * @param topCard     the card currently on top of the table pile,
     *                    or {@code null} if the table is empty
     */
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

    // =========================================================================
    // Human hand
    // =========================================================================

    /**
     * Rebuilds the human player's hand in the UI.
     *
     * <p>For each card in {@code cards}:
     * <ul>
     *   <li>A {@link CardView} is created face-up.</li>
     *   <li>If it is not the human's turn, or if the card is not legally
     *       playable at the current table sum, the card is visually disabled
     *       ({@link CardView#setCardDisabled(boolean)}).</li>
     *   <li>If it <em>is</em> the human's turn and the card <em>is</em>
     *       playable, a hover animation and a click handler (supplied by
     *       {@code clickHandlerFactory}) are attached.</li>
     * </ul>
     * </p>
     *
     * @param humanHand           the {@link HBox} that holds the human's card views;
     *                            cleared before repopulating
     * @param cards               the ordered list of cards in the human's hand
     * @param isHumanTurn         {@code true} when the human must play a card
     *                            (i.e., it is their turn AND they have not yet
     *                            drawn their replacement card this turn)
     * @param tableSum            the current running sum on the table; used to
     *                            determine which cards are legally playable
     * @param clickHandlerFactory a factory that, given a card's 0-based hand
     *                            index, returns the {@link EventHandler} to
     *                            attach to that card's view on click
     */
    public static void renderHumanHand(HBox humanHand,
                                        List<Card> cards,
                                        boolean isHumanTurn,
                                        int tableSum,
                                        java.util.function.IntFunction<EventHandler<MouseEvent>> clickHandlerFactory) {
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

    // =========================================================================
    // Machine hand
    // =========================================================================

    /**
     * Rebuilds one machine player's hand display with face-down card backs.
     *
     * <p>Clears {@code handBox} and adds one {@link CardView} per card,
     * rendered face-down so the human cannot see the machine's cards.
     * Also updates {@code countLabel} with the current hand size.</p>
     *
     * @param handBox       the {@link HBox} that holds this machine's card views;
     *                      cleared before repopulating
     * @param countLabel    label that shows the number of cards in this machine's hand
     * @param handSize      number of cards currently in the machine's hand
     * @param cardProvider  a function that returns the card at a given 0-based
     *                      hand index; used to create each {@link CardView}
     */
    public static void renderMachineHand(HBox handBox,
                                          Label countLabel,
                                          int handSize,
                                          java.util.function.Function<Integer, Card> cardProvider) {
        handBox.getChildren().clear();
        countLabel.setText(String.valueOf(handSize));

        for (int i = 0; i < handSize; i++) {
            Card card = cardProvider.apply(i);
            handBox.getChildren().add(new CardView(card, true));
        }
    }

    // =========================================================================
    // Status badges
    // =========================================================================

    /**
     * Updates a player's status badge label with the appropriate CSS class
     * and display text.
     *
     * <p>Exactly one of the following CSS classes is active at any time:
     * <ul>
     *   <li>{@code badge-eliminated} — player has been knocked out.</li>
     *   <li>{@code badge-active} — it is currently this player's turn.</li>
     *   <li>{@code badge-waiting} — player is alive but waiting for their turn.</li>
     * </ul>
     * All three classes are removed before the new one is added to avoid
     * accumulation.</p>
     *
     * @param label   the badge {@link Label} node to update
     * @param player  the player this badge represents
     * @param current the player whose turn it is right now
     */
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

    // =========================================================================
    // Elimination log
    // =========================================================================

    /**
     * Appends one entry to the elimination log panel (HU-5).
     *
     * <p>Creates a new {@link Label} with the CSS class {@code elim-entry}
     * and adds it to the bottom of {@code elimList}. Called once per
     * eliminated player, in the order they were knocked out.</p>
     *
     * @param elimList   the {@link VBox} that acts as the elimination log container
     * @param playerName display name of the eliminated player
     */
    public static void addEliminatedLogEntry(VBox elimList, String playerName) {
        Label entry = new Label("\u2718  " + playerName);
        entry.getStyleClass().add("elim-entry");
        elimList.getChildren().add(entry);
    }
}
