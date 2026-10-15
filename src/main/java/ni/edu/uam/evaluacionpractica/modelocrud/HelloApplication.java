package ni.edu.uam.evaluacionpractica.modelocrud;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.evaluacionpractica.utils.AlertUtil;
import ni.edu.uam.evaluacionpractica.utils.ConexionBD;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Views/MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 840);
        stage.setTitle("TecnoAsistencia Nicaragua S.A. - Gestión de Citas Técnicas");
        stage.setMinWidth(1080);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();

        if (!ConexionBD.probarConexion()) {
            AlertUtil.mostrarError("Error de conexión",
                    "No se pudo conectar con PostgreSQL. Verifique que el servidor esté en ejecución "
                            + "y que las credenciales (DB_URL, DB_USER, DB_PASSWORD) sean correctas.");
        }
    }

    @Override
    public void stop() {
        ConexionBD.cerrar();
    }
}
