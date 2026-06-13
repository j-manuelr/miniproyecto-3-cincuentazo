module np.cincuentazo {
    requires javafx.controls;
    requires javafx.fxml;


    opens np.cincuentazo to javafx.fxml;
    exports np.cincuentazo;
    exports np.cincuentazo.model;
    opens np.cincuentazo.model to javafx.fxml;
    exports np.cincuentazo.controller;
    opens np.cincuentazo.controller to javafx.fxml;
}