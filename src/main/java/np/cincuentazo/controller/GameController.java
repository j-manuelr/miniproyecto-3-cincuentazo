package np.cincuentazo.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;
import np.cincuentazo.exception.InvalidPlayException;
import np.cincuentazo.model.*;
import np.cincuentazo.thread.TurnTimerThread;
import np.cincuentazo.view.GameView;
import np.cincuentazo.view.UiAnimations;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Main FXML controller for the Cincuentazo game.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Reacts to user-input events declared in {@code game-view.fxml}.</li>
 *   <li>Delegates turn management to {@link TurnController}.</li>
 *   <li>Keeps every JavaFX node in sync with the {@link GameState} after each action.</li>
 *   <li>Manages the {@link TurnTimerThread} that gives the human player a time limit.</li>
 * </ul>
 *
 * <p><b>Threading contract:</b> every method in this class runs on the JavaFX
 * Application Thread. Callbacks from {@link np.cincuentazo.thread.MachinePlayerThread}
 * and {@link TurnTimerThread} use {@code Platform.runLater()} before calling
 * into this controller.
 *
 * <p><b>Keyboard shortcuts:</b> once the game scene is active, the player can
 * use keys 1–4 to play a card by position and SPACE/ENTER to draw from the deck.
 * Shortcuts are dispatched through a {@link #keyboardActions} lookup table
 * (an {@link EnumMap}) rather than a long {@code switch}, registered once via
 * an {@link javafx.scene.Scene} event <em>filter</em> attached through a
 * {@code sceneProperty()} listener — this keeps keyboard wiring independent
 * from {@link #startNewGame()} and safe against being attached before the
 * scene exists.</p>
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

    /**
     * Countdown timer for the human player's turn (30 seconds).
     * Started by {@link TurnController} when the human has valid plays.
     * Cancelled when the human plays a card or the game resets.
     */
    private TurnTimerThread humanTurnTimer;

    /**
     * Lookup table mapping each supported {@link KeyCode} to the action it
     * triggers. Built once in {@link #initialize()}; {@link #onKeyPressed(KeyEvent)}
     * does a single map lookup instead of branching through a {@code switch},
     * so adding a new shortcut later only means adding one map entry.
     *
     * <p>{@link EnumMap} is used (instead of a generic {@code HashMap}) because
     * the keys are a small, fixed enum ({@code KeyCode}) — it stores entries in
     * an array internally, giving faster, allocation-free lookups than hashing.
     */
    private final Map<KeyCode, Runnable> keyboardActions = buildKeyboardActions();

    /**
     * Event filter that dispatches key presses while the game scene is active.
     * Stored as a field (not a fresh method reference each time) so it can be
     * both added to a new scene and removed from the previous one.
     */
    private final EventHandler<KeyEvent> keyPressFilter = this::onKeyPressed;

    // =========================================================================
    // JavaFX lifecycle
    // =========================================================================

    /**
     * Called automatically by {@code FXMLLoader} once all {@code @FXML} fields
     * are injected. Sets up button hover animations.
     */
    @FXML
    public void initialize() {
        UiAnimations.applyButtonMotion(btnDrawCard);
        UiAnimations.applyButtonMotion(btnNewGame);
        UiAnimations.applyDeckMotion(deckPile);

        // Keyboard wiring is independent from game setup: attach/detach the
        // filter whenever this root pane's scene changes, instead of doing it
        // inside startNewGame(). An event *filter* (capturing phase) is used
        // instead of setOnKeyPressed so the shortcut always fires even if a
        // focused Button would otherwise be the one to react to SPACE/ENTER.
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressFilter);
            }
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressFilter);
            }
        });
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
    // FXML event handlers — mouse
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
     * "Nueva partida" button — cancels any active timer, stops the machine
     * thread, and returns to the start screen.
     */
    @FXML
    private void onNewGame() {
        cancelHumanTurnTimer();
        if (turnController != null) turnController.stopCurrentThread();
        navigateToStartScreen();
    }

    // =========================================================================
    // Keyboard event handler (HU — multiple event types)
    // =========================================================================

    /**
     * Handles keyboard shortcuts while the game scene is active.
     *
     * <ul>
     *   <li><b>1 / Numpad1</b> — play the first card in hand.</li>
     *   <li><b>2 / Numpad2</b> — play the second card in hand.</li>
     *   <li><b>3 / Numpad3</b> — play the third card in hand.</li>
     *   <li><b>4 / Numpad4</b> — play the fourth card in hand.</li>
     *   <li><b>SPACE / ENTER</b> — draw a card from the deck (after playing).</li>
     *   <li><b>N</b> — return to the start screen (same as "Nueva partida").</li>
     * </ul>
     *
     * <p>Looks up {@link #keyboardActions} for the pressed key; unmapped keys
     * are ignored at no extra cost. Recognized keys are consumed so they
     * cannot also trigger a focused control's own default behavior (e.g. a
     * focused {@link Button} reacting to SPACE/ENTER on its own).</p>
     *
     * @param event the keyboard event from the scene
     */
    private void onKeyPressed(KeyEvent event) {
        if (gameState == null || gameState.isGameOver()) return;

        Runnable action = keyboardActions.get(event.getCode());
        if (action != null) {
            action.run();
            event.consume();
        }
    }

    /**
     * Builds the immutable key-to-action lookup table used by {@link #onKeyPressed}.
     * Called once when the field is initialized.
     *
     * @return an {@link EnumMap} from {@link KeyCode} to the {@link Runnable}
     *         it triggers
     */
    private Map<KeyCode, Runnable> buildKeyboardActions() {
        Map<KeyCode, Runnable> actions = new EnumMap<>(KeyCode.class);
        actions.put(KeyCode.DIGIT1,  () -> humanPlayCard(0));
        actions.put(KeyCode.NUMPAD1, () -> humanPlayCard(0));
        actions.put(KeyCode.DIGIT2,  () -> humanPlayCard(1));
        actions.put(KeyCode.NUMPAD2, () -> humanPlayCard(1));
        actions.put(KeyCode.DIGIT3,  () -> humanPlayCard(2));
        actions.put(KeyCode.NUMPAD3, () -> humanPlayCard(2));
        actions.put(KeyCode.DIGIT4,  () -> humanPlayCard(3));
        actions.put(KeyCode.NUMPAD4, () -> humanPlayCard(3));
        actions.put(KeyCode.SPACE,   this::drawCardForHuman);
        actions.put(KeyCode.ENTER,   this::drawCardForHuman);
        actions.put(KeyCode.N,       this::onNewGame);
        return actions;
    }

    // =========================================================================
    // Navigation helpers
    // =========================================================================

    /**
     * Returns to {@code start-view.fxml} to allow a new game configuration (HU-1).
     * Delegates to {@link SceneNavigator}.
     */
    private void navigateToStartScreen() {
        SceneNavigator.toStartScreen(rootPane);
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
            eliminateCurrentPlayer();
        }
    }

    /**
     * Draws a card for the machine player after the play delay, then advances the turn.
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

        GameView.addEliminatedLogEntry(elimList, eliminated.getName());

        refreshView();

        if (gameState.isGameOver()) {
            navigateToVictoryScreen();
            return;
        }

        gameState.advanceTurn();
        turnController.processTurn();
    }

    // =========================================================================
    // Turn-timer management (TurnTimerThread — second required thread)
    // =========================================================================

    /**
     * Starts a 30-second countdown timer for the human player's turn.
     * Each tick updates {@code lblActionMsg} with the remaining seconds.
     * If the timer expires, the human player is automatically eliminated.
     *
     * <p>Called by {@link TurnController} when it confirms the human has at
     * least one valid card to play. A new timer replaces any previously
     * running one.</p>
     */
    public void startHumanTurnTimer() {
        cancelHumanTurnTimer();
        humanTurnTimer = new TurnTimerThread(
            30,
            remaining -> lblActionMsg.setText("⏱  " + remaining + "s — Selecciona una carta (teclas 1-4)"),
            this::onHumanTurnTimeout
        );
        humanTurnTimer.start();
    }

    /**
     * Cancels the human-turn timer if one is active.
     * The timeout callback will NOT fire after this call.
     * Safe to call even when no timer is running.
     */
    private void cancelHumanTurnTimer() {
        if (humanTurnTimer != null) {
            humanTurnTimer.cancel();
            humanTurnTimer = null;
        }
    }

    /**
     * Invoked by {@link TurnTimerThread} on the JavaFX thread when the 30-second
     * limit expires. Eliminates the human player for failing to act in time.
     */
    private void onHumanTurnTimeout() {
        if (gameState == null || gameState.isGameOver()) return;
        if (gameState.getCurrentPlayer() instanceof HumanPlayer) {
            lblActionMsg.setText("⏱  ¡Tiempo agotado! Jugador eliminado.");
            eliminateCurrentPlayer();
        }
    }

    // =========================================================================
    // Private — game setup
    // =========================================================================

    /**
     * Builds the player list, creates a fresh {@link GameState}, registers
     * keyboard shortcuts on the scene, and starts the first turn.
     */
    private void startNewGame() {
        cancelHumanTurnTimer();

        int numMachines = this.numberOfMachines;

        List<Player> players = new ArrayList<>();
        players.add(new HumanPlayer("Jugador"));
        for (int i = 1; i <= numMachines; i++) {
            players.add(new MachinePlayer("Máquina " + i));
        }

        gameState      = new GameState(players);
        waitingForDraw = false;
        drawInProgress = false;

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
     * Processes a human card click (HU-3), also triggered by number key shortcuts.
     * Cancels the turn timer on a successful play.
     *
     * @param index zero-based index of the card in the hand
     */
    private void humanPlayCard(int index) {
        if (waitingForDraw || drawInProgress || gameState.isGameOver()) return;

        Player current = gameState.getCurrentPlayer();
        if (!(current instanceof HumanPlayer human)) return;

        // Defensive bound check: keyboard shortcuts (1-4) are fixed regardless
        // of the hand's actual size, unlike mouse clicks which are only wired
        // to cards that exist. Silently ignore an out-of-range key press
        // instead of letting Hand.removeCard() throw IndexOutOfBoundsException.
        if (index < 0 || index >= human.getHand().getHandSize()) return;

        try {
            Card played = human.selectCard(index, gameState.getTableSum());
            cancelHumanTurnTimer();                     // card played — stop the countdown
            gameState.playCard(played);
            waitingForDraw = true;
            btnDrawCard.setDisable(false);
            refreshView();
            lblActionMsg.setText("¡Carta jugada!  Ahora toma una carta (SPACE / botón).");
        } catch (InvalidPlayException e) {
            lblActionMsg.setText("⚠  " + e.getMessage());
        }
    }

    /**
     * Draws a card from the deck for the human player (HU-4), then passes the turn.
     * Also triggered by SPACE / ENTER keyboard shortcuts.
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

    /** Starts the card-deal animation then calls {@link #finishHumanDraw()}. */
    private void animateDrawnCardToHand() {
        if (humanHand.getChildren().isEmpty()) {
            finishHumanDraw();
            return;
        }
        Node drawnCard = humanHand.getChildren().get(humanHand.getChildren().size() - 1);
        Platform.runLater(() ->
            UiAnimations.animateCardFromDeck(deckPile, drawnCard, this::finishHumanDraw)
        );
    }

    /** Resets draw flags and advances the turn after the animation completes. */
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
     *
     * <p>The action-message label is NOT overwritten while the
     * {@link TurnTimerThread} is actively counting down — the timer
     * updates that label every second.</p>
     */
    private void renderHumanHand() {
        List<Player> players = gameState.getPlayers();
        Player humanPlayer  = players.get(0);
        Player current      = gameState.getCurrentPlayer();
        boolean isHumanTurn = (current == humanPlayer) && !waitingForDraw && !drawInProgress;

        GameView.updateStatusBadge(lblHumanStatus, humanPlayer, current);

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
            if (drawInProgress) {
                lblActionMsg.setText("Tomando carta del mazo...");
            } else if (isHumanTurn) {
                // Only set default text if the timer is NOT already displaying a countdown.
                if (humanTurnTimer == null || humanTurnTimer.isCancelled()) {
                    lblActionMsg.setText("Selecciona una carta (clic o teclas 1-4).");
                }
            } else if (waitingForDraw) {
                lblActionMsg.setText("Toma una carta: clic en el mazo, botón o SPACE.");
            } else {
                lblActionMsg.setText("Esperando al siguiente jugador…");
            }
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
        GameView.updateStatusBadge(statusLabel, machine, current);
    }

    // =========================================================================
    // Private — game-over navigation (HU-6)
    // =========================================================================

    /** Navigates to the victory scene without blocking the JavaFX thread. */
    private void navigateToVictoryScreen() {
        Player winner     = gameState.getWinner();
        String winnerName = winner != null ? winner.getName() : "Nadie";
        boolean humanWon  = !gameState.getPlayers().isEmpty()
                            && winner == gameState.getPlayers().get(0);

        javafx.application.Platform.runLater(() ->
            SceneNavigator.toVictoryScreen(rootPane, humanWon, winnerName, numberOfMachines)
        );
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
