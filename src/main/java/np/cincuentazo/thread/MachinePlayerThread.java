package np.cincuentazo.thread;

import javafx.application.Platform;
import np.cincuentazo.controller.GameController;
import np.cincuentazo.model.MachinePlayer;

import java.util.Random;

/**
 * Background thread that introduces a 2–4 second thinking delay before a machine
 * player selects and plays a card (as required by HU-3).
 *
 * <p>After sleeping, the play action is dispatched back to the JavaFX Application
 * Thread via {@link Platform#runLater(Runnable)} to keep all UI and model access
 * thread-safe.
 */
public class MachinePlayerThread extends Thread {

    private final MachinePlayer machinePlayer;
    private final GameController gameController;
    private static final Random RANDOM = new Random();

    /**
     * Constructs a machine-player thread for the given player and controller.
     *
     * @param machinePlayer  the machine player whose turn it is
     * @param gameController the controller that will process the card play
     */
    public MachinePlayerThread(MachinePlayer machinePlayer, GameController gameController) {
        this.machinePlayer = machinePlayer;
        this.gameController = gameController;
        setDaemon(true); // does not prevent JVM shutdown
        setName("MachineThread-" + machinePlayer.getName());
    }

    /**
     * Sleeps between 2 and 4 seconds (simulating machine thinking), then
     * requests the controller to process the machine's play on the JavaFX thread.
     */
    @Override
    public void run() {
        try {
            int delayMs = 2000 + RANDOM.nextInt(2001); // [2 000 ms, 4 000 ms]
            Thread.sleep(delayMs);

            // All model/UI interactions must happen on the JavaFX Application Thread
            Platform.runLater(() -> gameController.processMachinePlay(machinePlayer));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted status
        }
    }
}
