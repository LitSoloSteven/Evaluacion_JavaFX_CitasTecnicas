module ni.edu.uam.evaluacionpractica.modelocrud {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.github.cdimascio.dotenv.java;
    requires java.sql;
    requires static lombok;


    opens ni.edu.uam.evaluacionpractica.modelocrud to javafx.fxml;
    exports ni.edu.uam.evaluacionpractica.modelocrud;
    exports ni.edu.uam.evaluacionpractica.modelocrud.Controllers;
    opens ni.edu.uam.evaluacionpractica.modelocrud.Controllers to javafx.fxml;
}