module np.cincuentazo {
//    requires javafx.controls;
//    requires javafx.fxml;
//    requires org.junit.jupiter.api;
//    requires org.junit.jupiter.params;
//
//
//    opens np.cincuentazo to javafx.fxml;
//    exports np.cincuentazo;
//    exports np.cincuentazo.model;
//    opens np.cincuentazo.model to javafx.fxml, org.junit.platform.commons;
//    exports np.cincuentazo.controller;
//    opens np.cincuentazo.controller to javafx.fxml;

    requires javafx.controls;
    requires javafx.fxml;

    opens np.cincuentazo to javafx.fxml;
    exports np.cincuentazo;

    exports np.cincuentazo.model;
    opens np.cincuentazo.model to javafx.fxml;

    exports np.cincuentazo.controller;
    opens np.cincuentazo.controller to javafx.fxml;
}

