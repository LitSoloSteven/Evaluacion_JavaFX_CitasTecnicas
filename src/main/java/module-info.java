module ni.edu.uam.evaluacionpractica.modelocrud {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires static lombok;
    requires io.github.cdimascio.dotenv.java;
    requires org.postgresql.jdbc;

    opens ni.edu.uam.evaluacionpractica.modelocrud to javafx.fxml;
    exports ni.edu.uam.evaluacionpractica.modelocrud;
    opens ni.edu.uam.evaluacionpractica.modelocrud.Controllers to javafx.fxml;
    exports ni.edu.uam.evaluacionpractica.modelocrud.Controllers;
}
