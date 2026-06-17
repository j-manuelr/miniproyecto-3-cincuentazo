package np.cincuentazo.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Start screen controller (HU-1).
 *
 * <p>Allows the player to choose how many machine opponents they want (1, 2 or 3)
 * via selection buttons ({@link ToggleButton}) grouped in a
 * {@link ToggleGroup}, and then loads the main game view
 * ({@code game-view.fxml}) passing that value to the {@link GameController}.
 *
 * <p>This class implements the Single Responsibility Principle (SRP): its
 * only responsibility is to capture the initial game configuration
 * and make the scene transition.
 */
public class StartController {

    @FXML private ToggleGroup  machineGroup;
    @FXML private ToggleButton btn1;
    @FXML private ToggleButton btn2;
    @FXML private ToggleButton btn3;

    /**
     * Ensures there is always a button selected (prevents deselection
     * by clicking the currently active one) and applies a listener for the same purpose.
     */
    @FXML
    public void initialize() {
        // Impedir que el usuario deseleccione el toggle activo
        machineGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                machineGroup.selectToggle(oldVal);
            }
        });
    }

    // =========================================================================
    // Manejador de eventos FXML
    // =========================================================================

    /**
     * "PLAY" button action (HU-1).
     *
     * <p>Reads the selected number of machines, loads {@code game-view.fxml},
     * injects the configuration into {@link GameController} and performs the
     * scene transition on the current {@link Stage}.
     */
    @FXML
    private void onStart() {
        int numMachines = getSelectedMachineCount();

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/np/cincuentazo/game-view.fxml")
            );
            Scene gameScene = new Scene(loader.load());

            GameController gameController = loader.getController();
            gameController.startGame(numMachines);

            Stage stage = (Stage) btn1.getScene().getWindow();
            stage.setScene(gameScene);
            stage.setTitle("Cincuentazo");

        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar game-view.fxml", e);
        }
    }

    // =========================================================================
    // Método privado auxiliar
    // =========================================================================

    /**
     * Returns the number of machine players indicated by the active toggle.
     * If none were selected (a situation prevented by the listener),
     * returns 1 as a safe default value.
     *
     * @return number of machines (1, 2 or 3)
     */
    private int getSelectedMachineCount() {
        Toggle selected = machineGroup.getSelectedToggle();
        if (selected instanceof ToggleButton tb) {
            return Integer.parseInt(tb.getText());
        }
        return 1;
    }
}
