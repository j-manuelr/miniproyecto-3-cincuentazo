package np.cincuentazo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicación Cincuentazo.
 *
 * <p>Carga {@code start-view.fxml} como primera escena (HU-1).
 * La transición a la vista del juego la realiza {@link np.cincuentazo.controller.StartController}
 * tras confirmar el número de oponentes.
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
