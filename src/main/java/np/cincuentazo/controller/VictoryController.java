package np.cincuentazo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import np.cincuentazo.view.UiAnimations;

/**
 * Controller for the victory/results screen shown after a game ends (HU-6).
 *
 * <p>Displays the game outcome — whether the human player won or lost — and
 * gives the option to play again with the same configuration or return to the
 * start screen to choose a new configuration.
 *
 * <p>SRP: this controller's only responsibility is displaying the result
 * and offering two navigation options. Scene transitions are delegated to
 * {@link SceneNavigator}.
 */
public class VictoryController {

    @FXML private Label  lblResultTitle;
    @FXML private Label  lblResultMessage;
    @FXML private Button btnPlayAgain;
    @FXML private Button btnStart;

    /** Number of machine players from the previous game, used for quick rematch. */
    private int numberOfMachines = 1;

    // =========================================================================
    // JavaFX lifecycle
    // =========================================================================

    /**
     * Called by {@code FXMLLoader} after all {@code @FXML} fields are injected.
     * Applies hover/press animations to the navigation buttons.
     */
    @FXML
    public void initialize() {
        UiAnimations.applyButtonMotion(btnPlayAgain);
        UiAnimations.applyButtonMotion(btnStart);
    }

    // =========================================================================
    // Public API — called by GameController after game over
    // =========================================================================

    /**
     * Configures the victory screen with the game result.
     *
     * @param humanWon         {@code true} if the human player is the sole survivor
     * @param winnerName       display name of the winning player
     * @param numberOfMachines number of machine opponents in the finished game;
     *                         stored so "Play Again" can reuse the same configuration
     */
    public void setResult(boolean humanWon, String winnerName, int numberOfMachines) {
        this.numberOfMachines = numberOfMachines;

        if (humanWon) {
            lblResultTitle.setText("¡GANASTE!");
            lblResultMessage.setText("Sobreviviste al cincuentazo.");
        } else {
            lblResultTitle.setText("PARTIDA TERMINADA");
            lblResultMessage.setText("Ganador: " + winnerName);
        }
    }

    // =========================================================================
    // FXML event handlers
    // =========================================================================

    /**
     * "Jugar de nuevo" button — starts a new game immediately with the same
     * number of machine opponents, skipping the start screen.
     * Delegates to {@link SceneNavigator#toGameScreen(javafx.scene.Node, int)}.
     */
    @FXML
    private void onPlayAgain() {
        SceneNavigator.toGameScreen(btnPlayAgain, numberOfMachines);
    }

    /**
     * "Inicio" button — navigates back to the start screen so the player
     * can choose a different number of machine opponents (HU-1).
     * Delegates to {@link SceneNavigator#toStartScreen(javafx.scene.Node)}.
     */
    @FXML
    private void onStart() {
        SceneNavigator.toStartScreen(btnStart);
    }
}
