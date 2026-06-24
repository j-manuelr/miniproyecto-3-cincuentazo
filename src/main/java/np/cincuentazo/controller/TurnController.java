package np.cincuentazo.controller;

import np.cincuentazo.model.GameState;
import np.cincuentazo.model.HumanPlayer;
import np.cincuentazo.model.MachinePlayer;
import np.cincuentazo.model.Player;
import np.cincuentazo.thread.MachinePlayerThread;

/**
 * Manages the turn cycle of a Cincuentazo game session.
 *
 * <p>On each turn:
 * <ul>
 *   <li><b>Human player:</b> checks whether a valid play exists. If not, the player
 *       is immediately eliminated and the turn advances. If yes, starts the
 *       {@link np.cincuentazo.thread.TurnTimerThread} via
 *       {@link GameController#startHumanTurnTimer()} so the human has a fixed
 *       window to act before being auto-eliminated.</li>
 *   <li><b>Machine player:</b> starts a {@link MachinePlayerThread} that introduces
 *       the required 2–4 s delay before the machine plays (HU-3).</li>
 * </ul>
 *
 * <p>All callbacks from {@link MachinePlayerThread} and
 * {@link np.cincuentazo.thread.TurnTimerThread} are posted to the JavaFX
 * Application Thread before reaching {@link GameController}, so no extra
 * synchronisation is required here.
 */
public class TurnController {

    private final GameState gameState;
    private final GameController gameController;
    private MachinePlayerThread currentMachineThread;

    /**
     * Constructs a {@code TurnController} linked to the given model and controller.
     *
     * @param gameState      the game-state model
     * @param gameController the main controller that handles UI updates and eliminations
     */
    public TurnController(GameState gameState, GameController gameController) {
        this.gameState      = gameState;
        this.gameController = gameController;
    }

    /**
     * Evaluates the current player and initiates the appropriate action.
     * Must be called on the JavaFX Application Thread.
     *
     * <p>Always refreshes the view first so the board reflects the new
     * current player before any thread or timer is launched.
     */
    public void processTurn() {
        if (gameState.isGameOver()) return;

        gameController.refreshView();

        Player current = gameState.getCurrentPlayer();

        if (current instanceof HumanPlayer human) {
            processHumanTurn(human);
        } else if (current instanceof MachinePlayer machine) {
            processMachineTurn(machine);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Checks whether the human player can make any valid move.
     * <ul>
     *   <li>If no valid move: delegates elimination to the controller (HU-5).</li>
     *   <li>If valid moves exist: starts the {@link np.cincuentazo.thread.TurnTimerThread}
     *       so the human has 30 seconds to act; the UI then waits for a card click
     *       or keyboard shortcut.</li>
     * </ul>
     *
     * @param human the human player whose turn it is
     */
    private void processHumanTurn(HumanPlayer human) {
        if (!human.canPlay(gameState.getTableSum())) {
            gameController.eliminateCurrentPlayer();
        } else {
            // Valid plays exist — start the countdown timer (second required thread).
            gameController.startHumanTurnTimer();
        }
    }

    /**
     * Stops any running machine thread, then starts a new {@link MachinePlayerThread}
     * for the given machine player.
     *
     * @param machine the machine player whose turn it is
     */
    private void processMachineTurn(MachinePlayer machine) {
        stopCurrentThread();
        currentMachineThread = new MachinePlayerThread(machine, gameController);
        currentMachineThread.start();
    }

    /**
     * Interrupts and discards the currently running machine thread, if any.
     * Safe to call even when no thread is active.
     */
    public void stopCurrentThread() {
        if (currentMachineThread != null && currentMachineThread.isAlive()) {
            currentMachineThread.interrupt();
            currentMachineThread = null;
        }
    }
}
