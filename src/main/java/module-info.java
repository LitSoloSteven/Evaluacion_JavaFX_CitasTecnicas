module ni.edu.uam.evaluacionpractica.modelocrud {
    requires javafx.controls;
    requires javafx.fxml;


    opens ni.edu.uam.evaluacionpractica.modelocrud to javafx.fxml;
    exports ni.edu.uam.evaluacionpractica.modelocrud;
    exports ni.edu.uam.evaluacionpractica.modelocrud.Controllers;
    opens ni.edu.uam.evaluacionpractica.modelocrud.Controllers to javafx.fxml;
}