package np.cincuentazo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import np.cincuentazo.view.UiAnimations;

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
 * and make the scene transition. Scene navigation is delegated to
 * {@link SceneNavigator}.
 */
public class StartController {

    @FXML private ToggleGroup  machineGroup;
    @FXML private ToggleButton btn1;
    @FXML private ToggleButton btn2;
    @FXML private ToggleButton btn3;
    @FXML private Button       btnStart;

    /**
     * Ensures there is always a button selected (prevents deselection
     * by clicking the currently active one) and applies a listener for the same purpose.
     */
    @FXML
    public void initialize() {
        UiAnimations.applyButtonMotion(btn1);
        UiAnimations.applyButtonMotion(btn2);
        UiAnimations.applyButtonMotion(btn3);
        UiAnimations.applyButtonMotion(btnStart);

        // Prevent the user from deselecting the currently active toggle
        machineGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                machineGroup.selectToggle(oldVal);
            }
        });
    }

    // =========================================================================
    // FXML event handler
    // =========================================================================

    /**
     * "PLAY" button action (HU-1).
     *
     * <p>Reads the selected number of machines and delegates the scene
     * transition to {@link SceneNavigator#toGameScreen(javafx.scene.Node, int)}.
     */
    @FXML
    private void onStart() {
        int numMachines = getSelectedMachineCount();
        SceneNavigator.toGameScreen(btn1, numMachines);
    }

    // =========================================================================
    // Private helper method
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
