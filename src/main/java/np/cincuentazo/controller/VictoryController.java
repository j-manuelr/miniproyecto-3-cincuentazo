package np.cincuentazo.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import np.cincuentazo.view.UiAnimations;

import java.io.IOException;

public class VictoryController {

    @FXML private Label lblResultTitle;
    @FXML private Label lblResultMessage;
    @FXML private Button btnPlayAgain;
    @FXML private Button btnStart;

    private int numberOfMachines = 1;

    @FXML
    public void initialize() {
        UiAnimations.applyButtonMotion(btnPlayAgain);
        UiAnimations.applyButtonMotion(btnStart);
    }

    public void setResult(boolean humanWon, String winnerName, int numberOfMachines) {
        this.numberOfMachines = numberOfMachines;

        if (humanWon) {
            lblResultTitle.setText("GANASTE");
            lblResultMessage.setText("Sobreviviste al cincuentazo.");
        } else {
            lblResultTitle.setText("PARTIDA TERMINADA");
            lblResultMessage.setText("Ganador: " + winnerName);
        }
    }

    @FXML
    private void onPlayAgain() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/np/cincuentazo/game-view.fxml")
            );
            Scene gameScene = new Scene(loader.load());

            GameController gameController = loader.getController();
            gameController.startGame(numberOfMachines);

            Stage stage = (Stage) btnPlayAgain.getScene().getWindow();
            stage.setScene(gameScene);
            stage.setTitle("Cincuentazo");
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar game-view.fxml", e);
        }
    }

    @FXML
    private void onStart() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/np/cincuentazo/start-view.fxml")
            );
            Scene startScene = new Scene(loader.load());

            Stage stage = (Stage) btnStart.getScene().getWindow();
            stage.setScene(startScene);
            stage.setTitle("Cincuentazo - Inicio");
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar start-view.fxml", e);
        }
    }
}
