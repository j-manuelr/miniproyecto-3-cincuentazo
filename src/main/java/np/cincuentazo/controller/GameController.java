package np.cincuentazo.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import np.cincuentazo.exception.InvalidPlayException;
import np.cincuentazo.model.*;
import np.cincuentazo.view.GameView;
import np.cincuentazo.view.UiAnimations;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Main FXML controller for the Cincuentazo game.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Reacts to user-input events declared in {@code game-view.fxml}.</li>
 *   <li>Delegates turn management to {@link TurnController}.</li>
 *   <li>Keeps every JavaFX node in sync with the {@link GameState} after each action.</li>
 * </ul>
 *
 * <p><b>Threading contract:</b> every method in this class runs on the JavaFX
 * Application Thread. Callbacks from {@link np.cincuentazo.thread.MachinePlayerThread}
 * use {@code Platform.runLater()} before calling into this controller.
 */
public class GameController {

    // =========================================================================
    // FXML-injected nodes (fx:id must match game-view.fxml exactly)
    // =========================================================================

    @FXML private BorderPane rootPane;
    @FXML private Label      lblCurrentTurn;
    @FXML private Label      lblDeckCount;

    // Machine zones -----------------------------------------------------------
    @FXML private HBox machinePlayersRow;
    @FXML private VBox machine1Zone;
    @FXML private VBox machine2Zone;
    @FXML private VBox machine3Zone;
    @FXML private Label lblMachine1;
    @FXML private Label lblMachine2;
    @FXML private Label lblMachine3;
    @FXML private HBox  machine1Hand;
    @FXML private HBox  machine2Hand;
    @FXML private HBox  machine3Hand;
    @FXML private Label lblMachine1Count;
    @FXML private Label lblMachine2Count;
    @FXML private Label lblMachine3Count;
    @FXML private Label lblMachine1Status;
    @FXML private Label lblMachine2Status;
    @FXML private Label lblMachine3Status;

    // Table area --------------------------------------------------------------
    @FXML private StackPane deckPile;
    @FXML private Label     lblDeckSize;
    @FXML private Label     lblTableSum;
    @FXML private Label     lblSumWarning;
    @FXML private StackPane tablePile;
    @FXML private Pane      tableCardDisplay;
    @FXML private Label     lblLastCard;

    // Human area --------------------------------------------------------------
    @FXML private Label  lblActionMsg;
    @FXML private HBox   humanHand;
    @FXML private Button btnDrawCard;
    @FXML private Button btnNewGame;
    @FXML private Label  lblHumanStatus;

    // Elimination log ---------------------------------------------------------
    @FXML private VBox eliminationLog;
    @FXML private VBox elimList;

    // =========================================================================
    // Internal state
    // =========================================================================

    private GameState      gameState;
    private TurnController turnController;

    /** Number of machine players chosen in the start screen (HU-1). */
    private int numberOfMachines = 1;

    /**
     * {@code true} after the human has played a card and still needs to draw
     * before the turn passes to the next player.
     */
    private boolean waitingForDraw = false;
    private boolean drawInProgress = false;

    // =========================================================================
    // JavaFX lifecycle
    // =========================================================================

    /**
     * Called automatically by {@code FXMLLoader} once all {@code @FXML} fields
     * are injected. Defers game start until the stage is visible.
     */
    @FXML
    public void initialize() {
        UiAnimations.applyButtonMotion(btnDrawCard);
        UiAnimations.applyButtonMotion(btnNewGame);
        UiAnimations.applyDeckMotion(deckPile);
        // La partida se inicia desde StartController mediante startGame().
    }

    /**
     * Public entry point invoked by {@link StartController} after
     * loading this controller from {@code start-view.fxml}.
     *
     * @param numMachines number of machine players (1–3)
     */
    public void startGame(int numMachines) {
        this.numberOfMachines = numMachines;
        Platform.runLater(this::startNewGame);
    }

    // =========================================================================
    // FXML event handlers
    // =========================================================================

    /**
     * Clicking the deck pile graphic acts as a shortcut for the "Tomar" button.
     */
    @FXML
    private void onDeckClicked() {
        drawCardForHuman();
    }

    /**
     * "Tomar" button — draws a card from the deck and passes the turn.
     */
    @FXML
    private void onDrawCard() {
        drawCardForHuman();
    }

    /**
     * "Nueva partida" button — stops any running machine thread and restarts.
     */
    @FXML
    private void onNewGame() {
        if (turnController != null) turnController.stopCurrentThread();
        navigateToStartScreen();
    }

    /**
     * Returns to {@code start-view.fxml} to allow a new game
     * configuration (HU-1). Called from the "Nueva partida" button.
     */
    private void navigateToStartScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/np/cincuentazo/start-view.fxml")
            );
            Scene startScene = new Scene(loader.load());
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(startScene);
            stage.setTitle("Cincuentazo – Inicio");
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar start-view.fxml", e);
        }
    }

    // =========================================================================
    // Public API — called by TurnController / MachinePlayerThread
    // =========================================================================

    /**
     * Processes a machine player's card-play action (HU-3).
     * Called on the JavaFX Application Thread via {@code Platform.runLater()}.
     *
     * @param machine the machine player whose turn it is
     */
    public void processMachinePlay(MachinePlayer machine) {
        if (gameState.isGameOver() || !machine.isAlive()) return;

        try {
            if (!machine.canPlay(gameState.getTableSum())) {
                eliminateCurrentPlayer();
                return;
            }

            int bestIdx = machine.chooseBestCardIndex(gameState.getTableSum());
            Card played = machine.selectCard(bestIdx, gameState.getTableSum());
            gameState.playCard(played);
            refreshView();

            // HU-4: machine draws 1–2 seconds after playing
            int drawDelay = 1000 + (int) (Math.random() * 1001);
            PauseTransition pause = new PauseTransition(Duration.millis(drawDelay));
            pause.setOnFinished(e -> processMachineDraw(machine));
            pause.play();

        } catch (InvalidPlayException ex) {
            // Safety net: should not reach here after canPlay() check
            eliminateCurrentPlayer();
        }
    }

    /**
     * Draws a card for the machine player after the play delay, then advances the turn.
     * Called by the {@link PauseTransition} inside {@link #processMachinePlay}.
     *
     * @param machine the machine player drawing a card
     */
    public void processMachineDraw(MachinePlayer machine) {
        if (gameState.isGameOver() || !machine.isAlive()) return;

        gameState.refillDeckIfNeeded();
        gameState.dealCardToPlayer(machine);
        refreshView();

        gameState.advanceTurn();
        turnController.processTurn();
    }

    /**
     * Eliminates the current player (HU-5), logs it, and advances the turn.
     * Package-private so {@link TurnController} can call it for the human player.
     */
    void eliminateCurrentPlayer() {
        Player eliminated = gameState.getCurrentPlayer();
        gameState.eliminatedCurrentPlayer();

        // Add entry to the elimination log
        GameView.addEliminatedLogEntry(elimList, eliminated.getName());

        refreshView();

        if (gameState.isGameOver()) {         // HU-6
            navigateToVictoryScreen();
            return;
        }

        gameState.advanceTurn();
        turnController.processTurn();
    }

    // =========================================================================
    // Private — game setup
    // =========================================================================

    /**
     * Asks how many machine opponents the player wants, then starts a fresh game (HU-1).
     */
    private void startNewGame() {
        int numMachines = this.numberOfMachines;

        List<Player> players = new ArrayList<>();
        players.add(new HumanPlayer("Jugador"));
        for (int i = 1; i <= numMachines; i++) {
            players.add(new MachinePlayer("Máquina " + i));
        }

        // GameState constructor deals 4 cards to each player and places the
        // initial table card automatically (HU-2).
        gameState      = new GameState(players);
        waitingForDraw = false;

        elimList.getChildren().clear();
        turnController = new TurnController(gameState, this);

        configureMachineZones(numMachines);
        refreshView();
        turnController.processTurn();
    }



    /**
     * Shows or hides each machine zone and updates machine names.
     *
     * @param numMachines number of machine players (1–3)
     */
    private void configureMachineZones(int numMachines) {
        List<Player> players = gameState.getPlayers();

        machine1Zone.setVisible(numMachines >= 1);
        machine1Zone.setManaged(numMachines >= 1);
        machine2Zone.setVisible(numMachines >= 2);
        machine2Zone.setManaged(numMachines >= 2);
        machine3Zone.setVisible(numMachines >= 3);
        machine3Zone.setManaged(numMachines >= 3);

        if (numMachines >= 1) lblMachine1.setText(players.get(1).getName());
        if (numMachines >= 2) lblMachine2.setText(players.get(2).getName());
        if (numMachines >= 3) lblMachine3.setText(players.get(3).getName());
    }

    // =========================================================================
    // Private — human turn logic
    // =========================================================================

    /**
     * Processes a human card click (HU-3).
     * Uses the inner {@link CardClickHandler} to validate and play the selected card.
     *
     * @param index zero-based index of the clicked card in the hand
     */
    private void humanPlayCard(int index) {
        if (waitingForDraw || gameState.isGameOver()) return;

        Player current = gameState.getCurrentPlayer();
        if (!(current instanceof HumanPlayer human)) return;

        try {
            Card played = human.selectCard(index, gameState.getTableSum());
            gameState.playCard(played);
            waitingForDraw = true;
            btnDrawCard.setDisable(false);
            refreshView();
            lblActionMsg.setText("¡Carta jugada!  Ahora toma una carta del mazo.");
        } catch (InvalidPlayException e) {
            lblActionMsg.setText("⚠  " + e.getMessage());
        }
    }

    /**
     * Draws a card from the deck for the human player (HU-4), then passes the turn.
     * Only executes when {@link #waitingForDraw} is {@code true}.
     */
    private void drawCardForHuman() {
        if (!waitingForDraw || drawInProgress || gameState.isGameOver()) return;

        Player current = gameState.getCurrentPlayer();
        if (!(current instanceof HumanPlayer)) return;

        drawInProgress = true;
        gameState.refillDeckIfNeeded();
        gameState.dealCardToPlayer(current);
        btnDrawCard.setDisable(true);
        refreshView();
        lblActionMsg.setText("Tomando carta del mazo...");

        animateDrawnCardToHand();
    }

    private void animateDrawnCardToHand() {
        if (humanHand.getChildren().isEmpty()) {
            finishHumanDraw();
            return;
        }

        Node drawnCard = humanHand.getChildren().get(humanHand.getChildren().size() - 1);
        Platform.runLater(() -> UiAnimations.animateCardFromDeck(deckPile, drawnCard, this::finishHumanDraw));
    }

    private void finishHumanDraw() {
        waitingForDraw = false;
        drawInProgress = false;
        gameState.advanceTurn();
        turnController.processTurn();
    }

    // =========================================================================
    // Private — view rendering
    // =========================================================================

    /**
     * Refreshes every visual element to match the current {@link GameState}.
     * Must be called on the JavaFX Application Thread.
     */
    public void refreshView() {
        updateHeaderLabels();
        updateTableArea();
        renderHumanHand();
        renderAllMachineHands();
    }

    /** Updates deck-size label, table-sum label, and the sum-warning message. */
    private void updateHeaderLabels() {
        int deckSize = gameState.getDeck().size();
        lblDeckSize.setText(String.valueOf(deckSize));
        lblDeckCount.setText(String.valueOf(deckSize));

        int sum = gameState.getTableSum();
        lblTableSum.setText(String.valueOf(sum));

        if      (sum >= 48) lblSumWarning.setText("¡Al límite!");
        else if (sum >= 40) lblSumWarning.setText("¡Cuidado!");
        else                lblSumWarning.setText("");

        Player current = gameState.getCurrentPlayer();
        lblCurrentTurn.setText(current != null ? current.getName() : "");
    }

    /** Updates the table pile display and the last-card text label. */
    private void updateTableArea() {
        Card top = gameState.getTopTableCard();
        GameView.renderTableCard(tablePile, lblLastCard, top);
    }

    /**
     * Rebuilds the human player's hand in the UI.
     * Cards are clickable only when it is the human's turn and they have not
     * yet drawn. Unplayable cards are rendered as disabled.
     * Uses the inner {@link CardClickHandler} class for mouse events.
     */
    private void renderHumanHand() {
        List<Player> players = gameState.getPlayers();
        Player humanPlayer  = players.get(0);
        Player current      = gameState.getCurrentPlayer();
        boolean isHumanTurn = (current == humanPlayer) && !waitingForDraw && !drawInProgress;

        updateStatusBadge(lblHumanStatus, humanPlayer, current);

        if (humanPlayer.isAlive()) {
            List<Card> cards = humanPlayer.getHand().getCards();
            GameView.renderHumanHand(
                humanHand, 
                cards, 
                isHumanTurn, 
                gameState.getTableSum(), 
                i -> new CardClickHandler(i)
            );
        } else {
            humanHand.getChildren().clear();
        }

        if (!gameState.isGameOver()) {
            if      (drawInProgress) lblActionMsg.setText("Tomando carta del mazo...");
            else if (isHumanTurn)  lblActionMsg.setText("Selecciona una carta para jugar.");
            else if (waitingForDraw) lblActionMsg.setText("Ahora toma una carta del mazo.");
            else                   lblActionMsg.setText("Esperando al siguiente jugador…");
        }
    }

    /** Refreshes all machine hand displays. */
    private void renderAllMachineHands() {
        List<Player> players = gameState.getPlayers();
        Player current       = gameState.getCurrentPlayer();

        if (players.size() > 1)
            renderMachineHand(players.get(1), machine1Hand, lblMachine1Count, lblMachine1Status, current);
        if (players.size() > 2)
            renderMachineHand(players.get(2), machine2Hand, lblMachine2Count, lblMachine2Status, current);
        if (players.size() > 3)
            renderMachineHand(players.get(3), machine3Hand, lblMachine3Count, lblMachine3Status, current);
    }

    /**
     * Rebuilds one machine player's hand display with face-down card backs.
     *
     * @param machine     the machine player
     * @param handBox     HBox holding the card views
     * @param countLabel  label showing number of cards in hand
     * @param statusLabel badge label for turn/waiting/eliminated state
     * @param current     player whose turn it currently is
     */
    private void renderMachineHand(Player machine, HBox handBox,
                                   Label countLabel, Label statusLabel,
                                   Player current) {
        int handSize = machine.getHand().getHandSize();
        GameView.renderMachineHand(
            handBox, 
            countLabel, 
            handSize, 
            i -> machine.getHand().getCard(i)
        );
        updateStatusBadge(statusLabel, machine, current);
    }

    /**
     * Updates a player's status badge with the correct CSS class and text.
     *
     * @param label   the badge label node
     * @param player  the player this badge belongs to
     * @param current the player whose turn it is
     */
    private void updateStatusBadge(Label label, Player player, Player current) {
        GameView.updateStatusBadge(label, player, current);
    }

    // =========================================================================
    // Private — game-over view (HU-6)
    // =========================================================================

    /** Navigates to the final result scene without blocking the JavaFX thread. */
    private void navigateToVictoryScreen() {
        Player winner    = gameState.getWinner();
        String winnerName = winner != null ? winner.getName() : "Nadie";
        boolean humanWon = !gameState.getPlayers().isEmpty() && winner == gameState.getPlayers().get(0);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/np/cincuentazo/victory-view.fxml")
                );
                Scene victoryScene = new Scene(loader.load());

                VictoryController victoryController = loader.getController();
                victoryController.setResult(humanWon, winnerName, numberOfMachines);

                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setScene(victoryScene);
                stage.setTitle("Cincuentazo - Resultado");
            } catch (IOException e) {
                throw new RuntimeException("No se pudo cargar victory-view.fxml", e);
            }
        });
    }

    // =========================================================================
    // Inner class — card click event handler
    // (satisfies rubric: "interfaces, clases internas y adaptadoras en eventos")
    // =========================================================================

    /**
     * Inner event-handler class that captures the card index at construction time
     * and delegates the click to {@link GameController#humanPlayCard(int)}.
     *
     * <p>Using a named inner class (instead of a lambda) gives access to the
     * enclosing controller's private state and satisfies the rubric requirement
     * for inner classes in event management.
     */
    private class CardClickHandler implements EventHandler<MouseEvent> {

        private final int cardIndex;

        /**
         * @param cardIndex zero-based index of the card in the human player's hand
         */
        CardClickHandler(int cardIndex) {
            this.cardIndex = cardIndex;
        }

        /**
         * Forwards the click to the enclosing controller.
         *
         * @param event the mouse event (unused)
         */
        @Override
        public void handle(MouseEvent event) {
            humanPlayCard(cardIndex);
        }
    }
}
