package np.cincuentazo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point of the Cincuentazo application.
 *
 * <p>Loads {@code start-view.fxml} as the first scene (HU-1).
 * The transition to the game view is performed by {@link np.cincuentazo.controller.StartController}
 * after the number of opponents has been confirmed.
 */
public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/np/cincuentazo/start-view.fxml")
        );
        Scene scene = new Scene(loader.load());
        primaryStage.setTitle("Cincuentazo – Inicio");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
