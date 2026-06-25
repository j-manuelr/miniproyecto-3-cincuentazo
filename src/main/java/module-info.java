/**
 * Main module for the Cincuentazo application.
 *
 * <p>Contains the model, view, controller, exception,
 * thread and interface packages used by the game.</p>
 */
module np.cincuentazo {
    requires javafx.controls;
    requires javafx.fxml;

    opens np.cincuentazo to javafx.fxml;
    exports np.cincuentazo;

    exports np.cincuentazo.model;
    opens np.cincuentazo.model to javafx.fxml;

    exports np.cincuentazo.controller;
    opens np.cincuentazo.controller to javafx.fxml;

    // view package: opened so javafx.fxml can instantiate view-based controllers
    // via reflection if they are ever used as fx:controller in an FXML file.
    exports np.cincuentazo.view;
    opens np.cincuentazo.view to javafx.fxml;
}
